-- SwarRoom Supabase Storage Setup (Bucket + Public Read/Upload/Update/Delete Policies)
-- Run this once in: Supabase Dashboard -> SQL Editor -> New Query -> Run

-- 1. Create (or update) the public 'swarroom-media' bucket without restrictive MIME blocks
-- (supports JPG, PNG, WEBP, GIF, MP4, MOV up to 50 MB)
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('swarroom-media', 'swarroom-media', true, 52428800, null)
on conflict (id) do update
set public = true,
    file_size_limit = 52428800,
    allowed_mime_types = null;

-- 2. Reset old policies for 'swarroom-media' safely
drop policy if exists "swarroom_media_public_select" on storage.objects;
drop policy if exists "swarroom_media_public_insert" on storage.objects;
drop policy if exists "swarroom_media_public_update" on storage.objects;
drop policy if exists "swarroom_media_public_delete" on storage.objects;

-- 3. Allow reading Gift DP, Gift Video, Emojis, User DP, Covers, and Moments
create policy "swarroom_media_public_select"
on storage.objects
for select
to public
using (bucket_id = 'swarroom-media');

-- 4. Allow uploading Gift Photos/DP, Videos, Emojis, User DP, Covers, and Moments
create policy "swarroom_media_public_insert"
on storage.objects
for insert
to public
with check (bucket_id = 'swarroom-media');

-- 5. Allow updating/overwriting objects in 'swarroom-media'
create policy "swarroom_media_public_update"
on storage.objects
for update
to public
using (bucket_id = 'swarroom-media')
with check (bucket_id = 'swarroom-media');

-- 6. Allow deleting Gift DP & Video files from Storage when deleted in Gift Panel
create policy "swarroom_media_public_delete"
on storage.objects
for delete
to public
using (bucket_id = 'swarroom-media');
