// Supabase Edge Function for Agora RTC Token Mode (Secured Mode: APP ID + App Certificate).
// Deploy in Supabase:
//   1. supabase secrets set AGORA_APP_ID=your_app_id AGORA_APP_CERTIFICATE=your_primary_certificate
//   2. supabase functions deploy agora-token --no-verify-jwt
// Then set AGORA_TOKEN_ENDPOINT=https://<project-ref>.supabase.co/functions/v1/agora-token
// in AI Studio Secrets & GitHub Actions Secrets.

import { RtcRole, RtcTokenBuilder } from "npm:agora-token@2.0.5";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const appId = (Deno.env.get("AGORA_APP_ID") ?? "").trim();
    const appCertificate = (Deno.env.get("AGORA_APP_CERTIFICATE") ?? "").trim();
    if (!appId || !appCertificate) {
      return new Response(
        JSON.stringify({ error: "AGORA_APP_ID or AGORA_APP_CERTIFICATE not set in Supabase Edge Function secrets." }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    const body = await req.json();
    const channelName = String(body?.channelName ?? "").replace(/[^A-Za-z0-9_-]/g, "_").slice(0, 64);
    const uid = Number.isInteger(body?.uid) && body.uid >= 0 ? body.uid : 0;
    const roleStr = String(body?.role ?? "publisher").toLowerCase();
    const role = roleStr === "subscriber" ? RtcRole.SUBSCRIBER : RtcRole.PUBLISHER;

    if (!channelName) {
      return new Response(
        JSON.stringify({ error: "channelName is required" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    const tokenExpireSeconds = 3600;
    const privilegeExpireSeconds = 3600;
    const token = RtcTokenBuilder.buildTokenWithUid(
      appId,
      appCertificate,
      channelName,
      uid,
      role,
      tokenExpireSeconds,
      privilegeExpireSeconds,
    );

    return new Response(
      JSON.stringify({
        token,
        uid,
        channelName,
        expiresInSeconds: tokenExpireSeconds,
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  } catch (err) {
    return new Response(
      JSON.stringify({ error: err instanceof Error ? err.message : "Failed to generate token" }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  }
});
