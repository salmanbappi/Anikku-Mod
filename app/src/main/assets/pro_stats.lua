-- Simple Pro Stats for Anikku
local active = false

function draw()
    if not active then return end
    
    local hwdec = mp.get_property("hwdec-current", "no")
    local passes = mp.get_property_number("vo-passes", 0)
    local interp = mp.get_property_native("interpolation")
    local fps = mp.get_property_number("estimated-display-fps", 0)
    local source = mp.get_property_number("container-fps", 0)
    if source == 0 then source = mp.get_property_number("video-params/fps", 0) end

    local text = "{\an7\fs12\shad1\bord1}"
    text = text .. "PRO STATISTICS (PAGE 6)\N"
    text = text .. "----------------------\N"
    text = text .. "Interpolation: " .. (interp and "ON" or "OFF") .. "\N"
    text = text .. "Status       : " .. (passes > 1 and "ACTIVE" or "STALLED") .. "\N"
    text = text .. "Display FPS  : " .. string.format("%.1f", fps) .. "\N"
    text = text .. "Source FPS   : " .. string.format("%.1f", source) .. "\N"
    text = text .. "Decoder      : " .. hwdec .. "\N"
    text = text .. "----------------------\N"
    text = text .. "Tip: mediacodec-copy required for Smooth Motion"

    mp.set_osd_ass(0, 0, text)
end

mp.register_script_message("display-page-6", function()
    active = true
    mp.add_periodic_timer(0.5, draw)
    draw()
end)

mp.register_script_message("hide-page-6", function()
    active = false
    mp.set_osd_ass(0, 0, "")
end)

