-- Native Pro Page 6 Statistics Script for Anikku
-- REMODEL: Event-based rendering for zero background overhead

local ass_header = [[{\an7\fs12\shad2\bord0\face(monospace)}]]
local active = false

local function draw()
    if not active then return end
    
    local hwdec = mp.get_property("hwdec-current", "no")
    local passes = mp.get_property_number("vo-passes", 0)
    local interp = mp.get_property_native("interpolation")
    local is_direct = (hwdec == "mediacodec")
    local is_working = interp and not is_direct and passes > 1
    
    local status = is_working and [[{\1c&H00FF00&}ACTIVE (Interpolating)]] or 
                   (is_direct and [[{\1c&H0000FF&}BYPASSED (Direct HWDEC)]] or [[{\1c&HAAAAAA&}OFF]])
    
    -- Frame Rates
    local source = mp.get_property_number("container-fps", 0)
    if source == 0 then source = mp.get_property_number("video-params/fps", 0) end
    
    local display = mp.get_property_number("estimated-display-fps", 0)
    local hz = mp.get_property_number("display-fps", 0)
    
    -- Shaders
    local shaders = mp.get_property("glsl-shaders", "")
    local shader_text = shaders == "" and "None" or (#(shaders:gsub("[^, যেত"]", "")) + 1 .. " Shaders Active")

    local o = ass_header
    o = o .. [[{\b1\c&H33BBFF&}PRO PLAYER STATISTICS (PAGE 6){\b0\c&HFFFFFF&}\N]]
    o = o .. "--------------------------------------\\N\\N"
    
    o = o .. [[{\c&H00FFFF&}[ Motion Pipeline ]{\c&HFFFFFF&}\N]]
    o = o .. string.format("% -15s: %s{\\c&HFFFFFF&}\N", "Status", status)
    o = o .. string.format("% -15s: %d passes\N", "Heartbeat", passes)
    o = o .. string.format("% -15s: %s\N\N", "Sync Mode", mp.get_property("video-sync", "n/a"))
    
    o = o .. [[{\c&H00FFFF&}[ Frame Rates ]{\c&HFFFFFF&}\N]]
    o = o .. string.format("% -15s: %.2f fps\N", "Source Rate", source)
    o = o .. string.format("% -15s: %.2f fps\N", "Render Rate", display)
    o = o .. string.format("% -15s: %.2f Hz\N\N", "Display Hz", hz)
    
    o = o .. [[{\c&H00FFFF&}[ Enhancement ]{\c&HFFFFFF&}\N]]
    o = o .. string.format("% -15s: %s\N\N", "Anime4K", shader_text)
    
    o = o .. [[{\c&H00FFFF&}[ Performance ]{\c&HFFFFFF&}\N]]
    o = o .. string.format("% -15s: %d ms\N", "Timing Delay", (mp.get_property_number("mistime", 0) * 1000))
    o = o .. string.format("% -15s: %d frames\N", "Dropped", mp.get_property_number("vo-delayed-frame-count", 0))
    o = o .. string.format("% -15s: %s\N\N", "Graphics API", mp.get_property("gpu-api", "n/a"):upper())
    
    local w = mp.get_property_number("video-out-params/w", 0)
    if w == 0 then w = mp.get_property_number("video-params/w", 0) end
    o = o .. [[{\c&H00FFFF&}[ Video Details ]{\c&HFFFFFF&}\N]]
    o = o .. string.format("% -15s: %dx%d\N", "Resolution", w, mp.get_property_number("video-out-params/h", 0))
    o = o .. string.format("% -15s: %s\N", "Decoder", hwdec)
    
    mp.set_osd_ass(0, 0, o)
end

-- Event Listeners (Triggers draw ONLY when needed)
local props = {"estimated-display-fps", "vo-passes", "vo-delayed-frame-count", "mistime", "glsl-shaders"}
for _, p in ipairs(props) do
    mp.observe_property(p, nil, draw)
end

mp.register_script_message("display-page-6", function()
    active = true
    draw()
end)

mp.register_script_message("hide-page-6", function()
    active = false
    mp.set_osd_ass(0, 0, "")
end)