-- SL-EL communications protocol dissector

-- declare our protocol
slelcomm_proto = Proto("slelcomm","IMSCF SL-EL communication")

local slelcomm_target_types = {"SUA", "SccpProvider", "ELRouter/query", "ELRouter/response", "DiameterStack", "DiameterGW"}
local slelcomm_content_types = {"SccpDataMessage", "SccpManagementMessage", "DiameterDataMessage"}
local slelcomm_query_status_types = {"notfound", "success"} -- "success,imscfCallId={imscfcallid},node={node}"

local f_target = ProtoField.new("Target", "slelcomm.target", ftypes.STRING, slelcomm_target_types)
local f_status = ProtoField.new("Status", "slelcomm.status", ftypes.STRING, slelcomm_query_status_types)
local f_content = ProtoField.new("Content", "slelcomm.content", ftypes.STRING, slelcomm_content_types)

slelcomm_proto.fields = { f_target, f_status, f_content }

local json_array_field = Field.new("json.array")

-- create a function to dissect it
function slelcomm_proto.dissector(buffer,pinfo,tree)

    local subtree = tree:add(slelcomm_proto, buffer(), "SL-EL communication")
    local start = 0
    local eolindex = string.find(buffer(0):string(), "\r\n") 
    local content = nil
    local sccp = nil
    local target = nil
    local status = nil
    
    while eolindex ~= 1 do
        local line = buffer(start, eolindex-1)
        if line:string():match("Target: .*") then
            pinfo.cols.protocol:append("/SLELCOMM")
            subtree:add(f_target, line(8))
            target = line(8):string()
        elseif line:string():match("Status: .*") then
            subtree:add(f_status, line(8))
            status = line(8):string()
        elseif line:string():match("Content: .*") then
            subtree:add(f_content, line(9))
            content = line(9):string()
        end
        start = start + eolindex + 1
        eolindex = string.find(buffer(start):string(), "\r\n")
    end
    
    if target:match("ELRouter/.*") then
        -- TODO process success/failure
    end

    if content=="SccpDataMessage" then
        sccp = true
    end

    -- only add payload if present
    if buffer:len() - start - 1 > 0 then
        local data = buffer(start + 1, buffer:len() - start - 1) 
        subtree:add(data, "Data:" .. data:string())
        if content then
            Dissector.get("json"):call(data:tvb(),pinfo,subtree)
            if sccp then
                local tcapByteArray = ByteArray.new()
                tcapByteArray:set_size(0)
                for b in string.gmatch(tostring(data:string():match(".*\"data\":%[([0-9%-,]+)%].*")), "%-?%d+") do
                    local bval = tonumber(b)
                    if bval < 0 then
                        bval = bval + 256 -- 2's complement
                    end
                    tcapByteArray:set_size(tcapByteArray:len()+1)
                    tcapByteArray:set_index(tcapByteArray:len()-1, bval)
                end
                local p = tostring(pinfo.cols.protocol)
                Dissector.get("tcap"):call(tcapByteArray:tvb(), pinfo, tree)
                pinfo.cols.protocol = p .. "/" .. tostring(pinfo.cols.protocol)
            end
        end
    else
        subtree:add(buffer(start + 1, 0), "No data")
    end

end
