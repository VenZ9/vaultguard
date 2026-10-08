package com.vaultguard.app.icon

data class ServiceInfo(
    val name: String,
    val domain: String,
    val packageName: String? = null,
    val brandColorHex: String = "#0284C7"
)

object ServiceMapping {
    private val services = listOf(
        ServiceInfo("Google", "google.com", "com.google.android.googlequicksearchbox", "#4285F4"),
        ServiceInfo("Gemini", "gemini.google.com", "com.google.android.apps.bard", "#1A73E8"),
        ServiceInfo("OpenAI", "openai.com", "com.openai.chatgpt", "#10A37F"),
        ServiceInfo("ChatGPT", "chatgpt.com", "com.openai.chatgpt", "#10A37F"),
        ServiceInfo("Claude", "claude.ai", "com.anthropic.claude", "#D97706"),
        ServiceInfo("Anthropic", "anthropic.com", "com.anthropic.claude", "#D97706"),
        ServiceInfo("GitHub", "github.com", "com.github.android", "#24292F"),
        ServiceInfo("GitLab", "gitlab.com", null, "#FC6D26"),
        ServiceInfo("Bitbucket", "bitbucket.org", null, "#0052CC"),
        ServiceInfo("Twitter", "x.com", "com.twitter.android", "#000000"),
        ServiceInfo("X", "x.com", "com.twitter.android", "#000000"),
        ServiceInfo("Discord", "discord.com", "com.discord", "#5865F2"),
        ServiceInfo("Slack", "slack.com", "com.Slack", "#4A154B"),
        ServiceInfo("AWS", "aws.amazon.com", "com.amazon.aws.console.mobile", "#FF9900"),
        ServiceInfo("Microsoft", "microsoft.com", "com.microsoft.azure", "#0078D4"),
        ServiceInfo("Azure", "azure.microsoft.com", "com.microsoft.azure", "#0078D4"),
        ServiceInfo("Stripe", "stripe.com", "com.stripe.android.dashboard", "#635BFF"),
        ServiceInfo("PayPal", "paypal.com", "com.paypal.android.p2pmobile", "#003087"),
        ServiceInfo("Spotify", "spotify.com", "com.spotify.music", "#1DB954"),
        ServiceInfo("Netflix", "netflix.com", "com.netflix.mediaclient", "#E50914"),
        ServiceInfo("YouTube", "youtube.com", "com.google.android.youtube", "#FF0000"),
        ServiceInfo("Reddit", "reddit.com", "com.reddit.frontpage", "#FF4500"),
        ServiceInfo("LinkedIn", "linkedin.com", "com.linkedin.android", "#0A66C2"),
        ServiceInfo("Facebook", "facebook.com", "com.facebook.katana", "#1877F2"),
        ServiceInfo("Instagram", "instagram.com", "com.instagram.android", "#E1306C"),
        ServiceInfo("Amazon", "amazon.com", "com.amazon.mShop.android.shopping", "#FF9900"),
        ServiceInfo("Notion", "notion.so", "notion.id", "#000000"),
        ServiceInfo("Figma", "figma.com", "com.figma.mirror", "#F24E1E"),
        ServiceInfo("Docker", "docker.com", null, "#2496ED"),
        ServiceInfo("Vercel", "vercel.com", null, "#000000"),
        ServiceInfo("Supabase", "supabase.com", null, "#3ECF8E"),
        ServiceInfo("Cloudflare", "cloudflare.com", "com.cloudflare.onedotonedotonedotone", "#F38020"),
        ServiceInfo("Linear", "linear.app", null, "#5E6AD2"),
        ServiceInfo("Zoom", "zoom.us", "us.zoom.videomeetings", "#0B5CFF"),
        ServiceInfo("Dropbox", "dropbox.com", "com.dropbox.android", "#0061FF"),
        ServiceInfo("Steam", "steampowered.com", "com.valvesoftware.android.steam.community", "#171A21"),
        ServiceInfo("Twitch", "twitch.tv", "tv.twitch.android.app", "#9146FF"),
        ServiceInfo("Apple", "apple.com", null, "#A2AAAD"),
        ServiceInfo("Uber", "uber.com", "com.ubercab", "#000000"),
        ServiceInfo("Airbnb", "airbnb.com", "com.airbnb.android", "#FF5A5F"),
        ServiceInfo("Telegram", "telegram.org", "org.telegram.messenger", "#24A1DE"),
        ServiceInfo("WhatsApp", "whatsapp.com", "com.whatsapp", "#25D366")
    )

    fun findService(query: String): ServiceInfo? {
        val clean = query.trim().lowercase()
        if (clean.isEmpty()) return null

        return services.find { service ->
            service.name.lowercase() == clean ||
            service.domain.lowercase() == clean ||
            service.domain.lowercase().contains(clean) ||
            clean.contains(service.name.lowercase()) ||
            service.packageName?.lowercase() == clean
        }
    }

    fun getAllKnownServices(): List<ServiceInfo> = services
}
