-- Native Page 6 Statistics Script for Anikku
-- Provides professional, high-performance OSD statistics with zero JNI overhead

local utils = require 'mp.utils'
local ass_header = "{\an7\fs12\shad2\bord0\face(monospace)}"
local active = false

function get_stats()
    local stats = {}
    
    -- Motion Pipeline
    local interp = mp.get_property_native("interpolation")
    local hwdec = mp.get_property("hwdec-current")
    local vo_passes = mp.get_property_number("vo-passes", 0)
    local sync = mp.get_property("video-sync")
    
    local is_direct = (hwdec == "mediacodec")
    local is_working = interp and not is_direct and vo_passes > 1
    
    stats.pipeline = is_working and "{\1c&H00FF00&}ACTIVE (Interpolating)" or 
                     (is_direct and "{\1c&H0000FF&}BYPASSED (Direct HWDEC)" or "OFF")
    stats.sync = sync
    stats.scaler = mp.get_property("tscale") or "none"
    
    -- Frame Rates
    local fps = mp.get_property_number("container-fps", 0)
    if fps == 0 then fps = mp.get_property_number("video-params/fps", 0) end
    
    stats.source = string.format("%.2f fps", fps)
    stats.render = string.format("%.2f fps", mp.get_property_number("estimated-vf-fps", 0))
    stats.display = string.format("%.2f fps", mp.get_property_number("estimated-display-fps", 0))
    stats.hz = string.format("%.2f Hz", mp.get_property_number("display-fps", 0))
    
    -- Performance
    stats.mistime = string.format("%d ms", (mp.get_property_number("mistime", 0) * 1000))
    stats.dropped = string.format("%d frames", mp.get_property_number("vo-delayed-frame-count", 0))
    stats.api = mp.get_property("gpu-api") or "n/a"
    
    -- Video Details
    local w = mp.get_property_number("dwidth", 0)
    local h = mp.get_property_number("dheight", 0)
    stats.res = string.format("%dx%d", w, h)
    stats.decoder = hwdec
    
    return stats
end

function draw_stats()
    if not active then return end
    
    local s = get_stats()
    local o = ass_header
    
    o = o .. "{\b1\c&H33BBFF&}PRO PLAYER STATISTICS (PAGE 6){\b0\c&HFFFFFF&}\N"
    o = o .. "--------------------------------------\N\N"
    
    o = o .. "{\c&H00FFFF&}[ Motion Pipeline ]{\c&HFFFFFF&}\N"
    o = o .. "Status         : " .. s.pipeline .. "{\c&HFFFFFF&}\N"
    o = o .. "Algorithm      : " .. s.scaler .. "\N"
    o = o .. "Sync Mode      : " .. s.sync .. "\N\N"
    
    o = o .. "{\c&H00FFFF&}[ Frame Rates ]{\c&HFFFFFF&}\N"
    o = o .. "Source Rate    : " .. s.source .. "\N"
    o = o .. "Render Rate    : " .. s.display .. "\N"
    o = o .. "Display Hz     : " .. s.hz .. "\N\N"
    
    o = o .. "{\c&H00FFFF&}[ Performance ]{\c&HFFFFFF&}\N"
    o = o .. "Mistime        : " .. s.mistime .. "\N"
    o = o .. "Dropped        : " .. s.dropped .. "\N"
    o = o .. "Graphics API   : " .. s.api .. "\N\N"
    
    o = o .. "{\c&H00FFFF&}[ Video Details ]{\c&HFFFFFF&}\N"
    o = o .. "Resolution     : " .. s.res .. "\N"
    o = o .. "Decoder        : " .. s.decoder .. "\N"
    
    mp.set_osd_ass(0, 0, o)
end

mp.register_script_message("display-page-6", function()
    active = true
    mp.add_periodic_timer(0.5, draw_stats)
    draw_stats()
end)

mp.register_script_message("hide-page-6", function()
    active = false
    mp.set_osd_ass(0, 0, "")
end)

