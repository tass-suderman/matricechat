package io.element.android.features.messages.impl

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DiscordRolesTest {
    private val tracking = """{"type":"fi.mau.discord.role_tracking","state_key":"","content":{"enabled":true}}"""
    private val role = """
        {
            "type": "fi.mau.discord.member_role",
            "state_key": "@discord_123:example.org",
            "content": {
                "role_id": "1",
                "name": "Moderator",
                "color": "#12ab34"
            }
        }
    """.trimIndent()

    @Test
    fun `enabled tracking renders the selected role and opaque RGB colour`() {
        val roles = parseDiscordRoles(listOf(tracking, role))
        assertThat(roles.getValue("@discord_123:example.org").name).isEqualTo("Moderator")
        assertThat(roles.getValue("@discord_123:example.org").color).isEqualTo(0xff12ab34L)
    }

    @Test
    fun `disabled or missing tracking ignores retained role events`() {
        assertThat(parseDiscordRoles(listOf(role))).isEmpty()
        assertThat(parseDiscordRoles(listOf(tracking.replace("true", "false"), role))).isEmpty()
    }

    @Test
    fun `cleared or malformed role metadata removes the colour`() {
        assertThat(parseDiscordRoles(listOf(tracking, role.replace("#12ab34", "red")))).isEmpty()
        assertThat(
            parseDiscordRoles(listOf(tracking, """{"type":"fi.mau.discord.member_role","state_key":"@discord_123:example.org","content":{}}"""))
        ).isEmpty()
        assertThat(parseDiscordRoles(listOf(tracking, "not json"))).isEmpty()
    }
    @Test
    fun `double puppets resolve through Discord per-message profile identity`() {
        val roles = parseDiscordRoles(listOf(tracking, role))
        val raw = """{"content":{"com.beeper.per_message_profile":{"id":"999_123"}}}"""
        assertThat(discordRoleUserId(raw, roles)).isEqualTo("@discord_123:example.org")
        assertThat(discordRoleUserId(raw.replace("999_123", "999_12"), roles)).isNull()
        assertThat(discordRoleUserId("{}", roles)).isNull()
    }
}
