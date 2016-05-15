-- LWCOMM protocol dissector

-- TODO
-- make numeric fields integer instead of string
-- append to pinfo.cols.protocol instead of resetting it every time
-- colouring retransmits and failovers


-- declare our protocol
lwcomm_proto = Proto("lwcomm","Lightweight UDP Communication")

local lwcomm_message_types = {"PAYLOAD", "ACK", "HEARTBEAT", "NACK"}

local f_reqline = ProtoField.new("Request line", "lwcomm.requestLine", ftypes.STRING)
local f_type = ProtoField.new("Message type", "lwcomm.type", ftypes.STRING, lwcomm_message_types)
local f_from = ProtoField.string("lwcomm.from", "From", FT_STRING)
local f_targetqueue = ProtoField.string("lwcomm.targetqueue", "Target-Queue", FT_STRING)
local f_retransmitcount = ProtoField.uint32("lwcomm.retransmitcount", "Retransmit-Count")
local f_failover = ProtoField.bool("lwcomm.failover", "Failover")
local f_messageid = ProtoField.string("lwcomm.messageid", "Message-id", FT_STRING)
local f_payloadbytes = ProtoField.uint32("lwcomm.payloadbytes", "Payload-Bytes", base.DEC)
local f_targetroute = ProtoField.string("lwcomm.targetroute", "Target-Route", FT_STRING)
local f_tag = ProtoField.string("lwcomm.tag", "Tag", FT_STRING)
local f_groupid = ProtoField.string("lwcomm.groupid", "Group-id", FT_STRING)

lwcomm_proto.fields = { f_reqline, f_type, f_from, f_targetqueue, f_retransmitcount, f_failover, f_messageid, f_payloadbytes, f_targetroute, f_tag, f_groupid }

local f_udp_dstport = Field.new("udp.dstport")

-- positions start with 1 from the end: [n]...[3][2][1] 
local function digit(number, pos)
    return (number - number%(10^(pos-1)))/(10^(pos-1))%10
end

-- Converts a port number to a node name, e.g. 32341 -> sfulEL03d.
-- udp_port is an integer
-- otherNode is an SL/EL node name that begins with the domain name to be used
local function nodeName(udp_port, otherNode)
    -- port rule: [layer] [domain] [instance] [node] [port_type(ignored)]
    local layers = {[1]="SL", [3]="EL"}
    --local domains = {[1]="sless", [2]="sful", [3]="perf"} -- these should be configurable
    local nodes = "abcdefghijklmnop"
    
    local lay = digit(udp_port,5)
    --local dom = digit(udp_port,4)
    local ins = digit(udp_port,3)
    local nod = digit(udp_port,2)

    local domain = string.match(otherNode, "(.+)[SE]L%d+%w+")

    return domain..layers[lay].."0"..ins..string.sub(nodes,nod,nod)   
end

-- create a function to dissect it
function lwcomm_proto.dissector(buffer,pinfo,tree)

    local subtree = tree:add(lwcomm_proto, buffer(), "LWCOMM")
    local start = 0
    local eolindex = string.find(buffer(0):string(), "\n") 
    local rltree = subtree:add(f_reqline, buffer(0, eolindex-1))

    if buffer(0, 4):string() == "ACK " then
        pinfo.cols.protocol = "LWCOMM ACK"
        rltree:add(f_type, buffer(0,3), lwcomm_message_types[2])
        rltree:add(f_messageid, buffer(4, 8))
    elseif buffer(0, 5):string() == "NACK " then
        pinfo.cols.protocol = "LWCOMM REJECT"
        rltree:add(f_type, buffer(0,4), lwcomm_message_types[4])
        rltree:add(f_messageid, buffer(5, 8))
    elseif buffer(0, 10):string() == "HEARTBEAT\n" then
        pinfo.cols.protocol = "LWCOMM HB"
        rltree:add(f_type, buffer(0,9), lwcomm_message_types[3])
    else
        pinfo.cols.protocol = "LWCOMM"
        rltree:add(f_type, buffer(0,8), lwcomm_message_types[1])
        rltree:add(f_messageid, buffer(0, 8))
    end

    start = eolindex
    eolindex = string.find(buffer(start):string(), "\n")
    local failover = false
    local retransmit = false
    local infotext = ""
    --subtree:add(buffer(0,1), "Debug start: " .. start .. " debug eolindex: " .. eolindex)
    while eolindex ~= 1 do
        --subtree:add(buffer(start, 4), "Debug header: " .. buffer(start, 4):string())
        if eolindex >= 4 and buffer(start, 4):string() == "From" then
            local from_buf = buffer(start + 6, eolindex - 7)
            subtree:add(f_from, from_buf)
            local sourceNode = from_buf:string()
            infotext = sourceNode.." -> "..nodeName(f_udp_dstport()(), sourceNode)
            pinfo.cols.info = infotext
        elseif eolindex >= 12 and buffer(start, 12):string() == "Target-Queue" then
            subtree:add(f_targetqueue, buffer(start + 14, eolindex - 15))
        elseif eolindex >= 16 and buffer(start, 16):string() == "Retransmit-Count" then
            retransmit = true
            local bytes = buffer(start + 18, eolindex - 19)
            subtree:add(f_retransmitcount, bytes, tonumber(bytes:string()))
        elseif eolindex >= 13 and buffer(start, 13):string() == "Payload-Bytes" then
            local bytes = buffer(start + 15, eolindex - 16)
            subtree:add(f_payloadbytes, bytes, tonumber(bytes:string()))
            -- from wireshark 1.11.3, number strings can be automatically decoded:
            -- subtree:add_packet_field(f_payloadbytes, bytes, ENC_IMSCFII + ENC_STRING)
        elseif eolindex >= 14 and buffer(start, 14):string() == "Failover: true" then
            subtree:add(f_failover, buffer(start, eolindex - 1), 1)
            failover = true
        elseif eolindex >= 14 and buffer(start, 14):string() == "Target-Route: " then
            subtree:add(f_targetroute, buffer(start + 14, eolindex - 15))
        elseif eolindex >= 5 and buffer(start, 5):string() == "Tag: " then
            subtree:add(f_tag, buffer(start + 5, eolindex - 6))
        elseif eolindex >= 10 and buffer(start, 10):string() == "Group-Id: " then
            subtree:add(f_groupid, buffer(start + 10, eolindex - 11))
        end
        start = start + eolindex
        eolindex = string.find(buffer(start):string(), "\n")
        --subtree:add(buffer(0,1), "Debug start: " .. start .. " debug eolindex: " .. eolindex)
    end
    -- TODO use appends instead of change in pinfo.cols.protocol
    if failover and retransmit then
        pinfo.cols.protocol = "LWCOMM FAILOVER RETRANS"
    elseif failover then
        pinfo.cols.protocol = "LWCOMM FAILOVER"
    elseif retransmit then
        pinfo.cols.protocol = "LWCOMM RETRANS"
    end
    -- only add payload if present
    if buffer:len() - start - 1 > 0 then
        -- TODO: handle possible subdissectors with dissector table or a header indicating protocol
        local slelcomm = Dissector.get("slelcomm")
        if slelcomm then
            slelcomm:call(buffer(start+1):tvb(),pinfo,tree)
            pinfo.cols.info:prepend(infotext.." ")
        else
            subtree:add(buffer(start + 1, buffer:len() - start - 1), "Payload:\n" .. buffer(start + 1, buffer:len() - start - 1):string())
        end
    else
        subtree:add(buffer(start + 1, 0), "No payload")
    end
    --subtree:add(buffer(0,2),"The first two bytes: " .. buffer(0,2):uint())
    --subtree = subtree:add(buffer(2,2),"The next two bytes")
    --subtree:add(buffer(2,1),"The 3rd byte: " .. buffer(2,1):uint())
    --subtree:add(buffer(3,1),"The 4th byte: " .. buffer(3,1):uint())
end


-- ######### Preferences handling #########

-- wireshark preferences are stored and parsed into this table
local current_settings = {
    udp_ports = "", -- preference value as string
    udp_ranges = {} -- ranges parsed from preference
}

-- register a range type preference for this protocol
lwcomm_proto.prefs["udp_ports"] = Pref.range("LWCOMM UDP port ranges", "10000-14000,30000-34000", "Range of UDP ports used by LWCOMM", 65535)

-- helper methods
local function register_udp_port_range(p_start, p_end)
    udp_table = DissectorTable.get("udp.port")
    for port = p_start, p_end do
        udp_table:add(port, lwcomm_proto)
    end
end

local function unregister_udp_port_range(p_start, p_end)
    udp_table = DissectorTable.get("udp.port")
    for port = p_start, p_end do
        udp_table:remove(port, lwcomm_proto)
    end
end

-- handle preference changes
function lwcomm_proto.init(arg1, arg2)
  for pref_name, old_v in pairs(current_settings) do
    local new_v = lwcomm_proto.prefs["udp_ports"]
    if old_v ~= new_v then
      if pref_name == "udp_ports" then

        -- unregister old ports
        for i,range in ipairs(current_settings.udp_ranges) do
          unregister_udp_port_range(range.p_start, range.p_end)
        end

        -- parse new ranges from preference value
        current_settings.udp_ports = new_v
        local rangeCount = 0
        for token in string.gmatch(new_v, "[%d-]+") do
          -- print("Found token: " .. token)
          local s, e
          rangeCount = rangeCount + 1
          s, e = string.match(token, "([%d]+)-([%d]+)")
          if e and s then
            -- print("Range of ports: " .. s .. " -> " .. e)
          else
            s = string.match(token, "[%d]+")
            if s then
              -- print("Single port: " .. s)
              e = s
            else
              print("Unrecognized range specification format!")
            end
          end
          if s and e then
            current_settings.udp_ranges[rangeCount] = { p_start=tonumber(s), p_end=tonumber(e) }
          end
        end

        -- register new ports
        for i,range in ipairs(current_settings.udp_ranges) do
          print("Registering for range ".. range.p_start .. " -> " .. range.p_end)
          register_udp_port_range(range.p_start, range.p_end)
        end

      end -- udp_ports
    end -- changed value
  end -- for prefs
end

-- ######## End preferences handling ########
