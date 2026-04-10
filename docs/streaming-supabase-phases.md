# Streaming + Supabase rollout phases

## Phase 1 - Local and API-ready streaming (done in code)
- Endpoint: `GET /api/videos/{movieId}`
- Supports `Range` header and returns:
  - `206 Partial Content` when header exists
  - `200 OK` when full file requested
- Supports both local path and remote URL (Supabase/S3 signed/public URL)
- Uses `StreamingResponseBody` and chunked reads to avoid loading entire file into memory

## Phase 2 - Supabase object storage integration
- Create bucket `movies` in Supabase Storage
- Upload structure:
  - `movies/{movieKey}/720p.mp4`
  - `movies/{movieKey}/1080p.mp4`
  - `movies/{movieKey}/thumbnail.jpg`
- Save only URL/object key in DB (`movies.video_url`)
- For private bucket, backend should generate signed URL before streaming redirect

## Phase 3 - Production optimization (recommended)
- Prefer direct playback from CDN + signed URL:
  1. Backend authenticates user and validates watch permission
  2. Backend returns signed URL (short TTL, e.g. 60-120s)
  3. Frontend player requests CDN URL directly
- Advantages:
  - Reduced backend bandwidth and CPU
  - Better latency and buffering under peak traffic

## HTTP Range behavior summary
- Request example:
  - `Range: bytes=0-1048575`
- Required response headers:
  - `Content-Type: video/mp4`
  - `Accept-Ranges: bytes`
  - `Content-Length`
  - `Content-Range` (for partial responses)

## HLS migration path
- Segment MP4 into `.m3u8` + `.ts` or `.m4s` chunks using FFmpeg
- Serve playlist and chunks from object storage/CDN
- Keep auth as tokenized signed URL at playlist/chunk level
- This is the preferred architecture for adaptive bitrate streaming
