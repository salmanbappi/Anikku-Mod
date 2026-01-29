-- Native Pro Page 6 Statistics Script for Anikku
-- Remodeled for peak accuracy and zero-lag performance

local ass_header = [[{\an7\fs12\shad2\bord0\face(monospace)}]]
local active = false

function get_stats()
    local s = {}
    
    -- Sync & Pipeline
    local hwdec = mp.get_property("hwdec-current", "no")
    local passes = mp.get_property_number("vo-passes", 0)
    local interp = mp.get_property_native("interpolation")
    local is_direct = (hwdec == "mediacodec")
    local is_working = interp and not is_direct and passes > 1
    
    s.status = is_working and [[{\1c&H00FF00&}ACTIVE (Interpolating)]] or 
               (is_direct and [[{\1c&H0000FF&}BYPASSED (Direct HWDEC)]] or [[{\1c&HAAAAAA&}OFF]])
    s.passes = passes
    s.sync = mp.get_property("video-sync", "n/a")
    s.api = mp.get_property("gpu-api", "n/a"):upper()
    
    -- Frame Rates
    local source = mp.get_property_number("container-fps", 0)
    if source == 0 then source = mp.get_property_number("video-params/fps", 0) end
    s.source = string.format("%.2f fps", source)
    s.display = string.format("%.2f fps", mp.get_property_number("estimated-display-fps", 0))
    s.hz = string.format("%.2f Hz", mp.get_property_number("display-fps", 0))
    
    -- Timing
    s.mistime = string.format("%d ms", (mp.get_property_number("mistime", 0) * 1000))
    s.dropped = string.format("%d frames", mp.get_property_number("vo-delayed-frame-count", 0))
    
    -- Video Size (3-layer fallback for 0x0 fix)
    local w = mp.get_property_number("video-out-params/w", 0)
    if w == 0 then w = mp.get_property_number("video-params/w", 0) end
    if w == 0 then w = mp.get_property_number("dwidth", 0) end
    
    local h = mp.get_property_number("video-out-params/h", 0)
    if h == 0 then h = mp.get_property_number("video-params/h", 0) end
    if h == 0 then h = mp.get_property_number("dheight", 0) end
    s.res = string.format("%dx%d", w, h)
    s.decoder = hwdec
    
    return s
end

function draw()
    if not active then return end
    local s = get_stats()
    local o = ass_header
    
    o = o .. [[{\b1\c&H33BBFF&}PRO PLAYER STATISTICS (PAGE 6){\b0\c&HFFFFFF&}\N]]
    o = o .. "--------------------------------------\\N\\N"
    
    o = o .. [[{\c&H00FFFF&}[ Motion Pipeline ]{\c&HFFFFFF&}\N]]
    o = o .. "Status         : " .. s.status .. [[{\c&HFFFFFF&}\N]]
    o = o .. "Heartbeat      : " .. s.passes .. " passes\\N"
    o = o .. "Sync Mode      : " .. s.sync .. "\\N\\N"
    
    o = o .. [[{\c&H00FFFF&}[ Frame Rates ]{\c&HFFFFFF&}\N]]
    o = o .. "Source Rate    : " .. s.source .. "\\N"
    o = o .. "Render Rate    : " .. s.display .. "\\N"
    o = o .. "Display Hz     : " .. s.hz .. "\\N\\N"
    
    o = o .. [[{\c&H00FFFF&}[ Performance ]{\c&HFFFFFF&}\N]]
    o = o .. "Timing Delay   : " .. s.mistime .. "\\N"
    o = o .. "Dropped        : " .. s.dropped .. "\\N"
    o = o .. "Graphics API   : " .. s.api .. "\\N\\N"
    
    o = o .. [[{\c&H00FFFF&}[ Video Details ]{\c&HFFFFFF&}\N]]
    o = o .. "Resolution     : " .. s.res .. "\\N"
    o = o .. "Decoder        : " .. s.decoder .. "\\N"
    
    mp.set_osd_ass(0, 0, o)
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