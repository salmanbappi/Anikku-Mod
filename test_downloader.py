import asyncio
import httpx
import os
import time
import sys
from urllib.parse import urlparse

# CONFIGURATION
# Replace this with a REAL m3u8 link from HiAnime if you have one, 
# otherwise I will use a test HLS stream.
M3U8_URL = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8" 
USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
THREADS = 4

async def download_segment(client, url, index, semaphore, progress_data):
    async with semaphore:
        for attempt in range(5):
            try:
                # Mimic AniZen Headers
                parsed_uri = urlparse(url)
                origin = f"{parsed_uri.scheme}://{parsed_uri.netloc}"
                headers = {
                    "User-Agent": USER_AGENT,
                    "Referer": f"{origin}/",
                    "Origin": origin
                }
                
                response = await client.get(url, headers=headers)
                response.raise_for_status()
                data = response.content
                
                # Simulate Writing
                progress_data['downloaded'] += 1
                progress_data['bytes'] += len(data)
                return index, data
            except Exception as e:
                # print(f"Retry {attempt} for seg {index}: {e}")
                await asyncio.sleep(1)
        raise Exception(f"Failed seg {index}")

async def main():
    print(f"🚀 Starting Real-Time Downloader Test...")
    print(f"Target: {M3U8_URL}")
    
    async with httpx.AsyncClient() as client:
        # 1. Fetch Playlist
        print("1. Fetching Playlist...")
        resp = await client.get(M3U8_URL, headers={"User-Agent": USER_AGENT})
        content = resp.text
        
        base_url = M3U8_URL.rsplit('/', 1)[0] + "/"
        segments = []
        for line in content.splitlines():
            if line and not line.startswith("#"):
                if line.startswith("http"):
                    segments.append(line)
                else:
                    segments.append(base_url + line)
        
        print(f"✅ Found {len(segments)} segments.")
        
        # 2. Parallel Download
        print("2. Starting Parallel Download (4 Threads)...")
        start_time = time.time()
        semaphore = asyncio.Semaphore(THREADS)
        progress = {'downloaded': 0, 'bytes': 0}
        
        # Start UI loop
        async def ui_loop():
            while progress['downloaded'] < len(segments):
                elapsed = time.time() - start_time
                if elapsed > 0:
                    speed = progress['bytes'] / elapsed / 1024 / 1024 # MB/s
                    sys.stdout.write(f"\r⚡ Speed: {speed:.2f} MB/s | Segments: {progress['downloaded']}/{len(segments)} | Size: {progress['bytes']/1024/1024:.2f} MB")
                    sys.stdout.flush()
                await asyncio.sleep(0.5)
        
        ui_task = asyncio.create_task(ui_loop())
        
        tasks = [download_segment(client, seg, i, semaphore, progress) for i, seg in enumerate(segments)]
        results = await asyncio.gather(*tasks)
        
        ui_task.cancel()
        print("\n\n3. Verifying Merge...")
        
        # Sort and "Write"
        results.sort(key=lambda x: x[0])
        total_size = sum(len(x[1]) for x in results)
        
        print(f"✅ Download Complete!")
        print(f"📦 Final Size: {total_size / 1024 / 1024:.2f} MB")
        print(f"⏱️ Time Taken: {time.time() - start_time:.2f}s")
        
        # 0MB Check
        if total_size == 0:
            print("❌ FAILURE: 0MB File Detected (Bug Reproduced)")
        else:
            print("✅ SUCCESS: Data verified.")

if __name__ == "__main__":
    if "hianime" in M3U8_URL:
        print("⚠️ NOTE: For real HiAnime test, please provide a fresh m3u8 link as they expire quickly.")
    asyncio.run(main())
