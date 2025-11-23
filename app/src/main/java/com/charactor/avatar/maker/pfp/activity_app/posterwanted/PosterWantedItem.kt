package com.charactor.avatar.maker.pfp.activity_app.posterwanted

import com.charactor.avatar.maker.pfp.data.model.TemplateConfigProvider

/**
 * Data model for Poster Wanted Template item
 */
data class PosterWantedItem(
    val id: Int,
    val templateId: Int,        // 1-16
    val avatarId: Int,          // 1-25
    val name: String,
    val bounty: String
) {
    /**
     * Get avatar asset path for Glide
     */
    fun getAvatarPath(): String {
        return "file:///android_asset/avatar/$avatarId.png"
    }

    /**
     * Get template item path for Glide
     */
    fun getTemplatePath(): String {
        return "file:///android_asset/template/$templateId/item.png"
    }

    /**
     * Get full bounty text with prefix and suffix from template config
     * Example: bounty="30,000,000" + prefix="$" + suffix="" → "$30,000,000"
     */
    fun getFullBountyText(): String {
        val config = TemplateConfigProvider.getConfig(templateId)
        return config.bountyPrefix + bounty + config.bountySuffix
    }

    companion object {
        // Random pirate names
        private val PIRATE_NAMES = listOf(
            "MONKEY D. LUFFY",
            "RORONOA ZORO",
            "NAMI",
            "USOPP",
            "SANJI",
            "TONY CHOPPER",
            "NICO ROBIN",
            "FRANKY",
            "BROOK",
            "JINBE",
            "SHANKS",
            "BLACKBEARD",
            "KAIDO",
            "BIG MOM",
            "WHITEBEARD",
            "GOL D. ROGER",
            "PORTGAS D. ACE",
            "TRAFALGAR LAW",
            "EUSTASS KID",
            "DRACULE MIHAWK",
            "BOA HANCOCK",
            "CROCODILE",
            "DONQUIXOTE",
            "KATAKURI",
            "MARCO",
            "SABO",
            "BUGGY",
            "SMOKER",
            "AOKIJI",
            "KIZARU",
            "AKAINU",
            "FUJITORA",
            "GARP",
            "SENGOKU",
            "RAYLEIGH",
            "CAPTAIN JACK",
            "BLACKHEART",
            "REDBEARD",
            "STORMWIND",
            "SEAWOLF"
        )

        // Random bounty amounts
        private val BOUNTY_AMOUNTS = listOf(
            "30,000,000",
            "50,000,000",
            "100,000,000",
            "200,000,000",
            "300,000,000",
            "500,000,000",
            "800,000,000",
            "100,000,000",
            "150,000,000",
            "250,000,000",
            "350,000,000",
            "4,000,000",
            "550,000,000",
            "10,000,000",
            "15,000,000",
            "66,000,000",
            "77,000,000",
            "88,000,000",
            "320,000,000",
            "438,000,000",
            "30,000,000",
            "10,000,000",
            "15,000,000",
            "20,000,000",
            "40,000,000",
            "50,000,000",
            "1",
            "10",
            "100",
            "1,000",
            "10,000",
            "55,000,000",
            "666,666",
            "7,777,777",
            // Thêm giá trị từ 100M đến 1B
            "120,000,000",
            "150,000,000",
            "180,000,000",
            "220,000,000",
            "260,000,000",
            "290,000,000",
            "330,000,000",
            "370,000,000",
            "400,000,000",
            "450,000,000",
            "480,000,000",
            "520,000,000",
            "560,000,000",
            "600,000,000",
            "650,000,000",
            "700,000,000",
            "750,000,000",
            "820,000,000",
            "850,000,000",
            "900,000,000",
            "950,000,000",
            "999,999,999"
        )
        /**
         * Generate random poster wanted items
         * Uses maxBountyLength from template config to limit bounty length
         */
        fun generateRandomItems(count: Int): List<PosterWantedItem> {
            val items = mutableListOf<PosterWantedItem>()
            val random = java.util.Random()

            for (i in 1..count) {
                val templateId = random.nextInt(16) + 1  // 1-16

                // Get template config to check maxBountyLength
                val config = TemplateConfigProvider.getConfig(templateId)
                val maxLength = config.maxBountyLength

                // Filter bounty amounts based on maxBountyLength
                val availableBounties = if (maxLength != null) {
                    // Limit to bounties with length <= maxLength
                    BOUNTY_AMOUNTS.filter { it.length <= maxLength }
                } else {
                    // No limit - use all bounties
                    BOUNTY_AMOUNTS
                }

                // Select random bounty from available options
                val selectedBounty = if (availableBounties.isNotEmpty()) {
                    availableBounties[random.nextInt(availableBounties.size)]
                } else {
                    // Fallback: use shortest bounty if filter is too strict
                    BOUNTY_AMOUNTS.minByOrNull { it.length } ?: BOUNTY_AMOUNTS[0]
                }

                items.add(
                    PosterWantedItem(
                        id = i,
                        templateId = templateId,
                        avatarId = random.nextInt(25) + 1,        // 1-25
                        name = PIRATE_NAMES[random.nextInt(PIRATE_NAMES.size)],
                        bounty = selectedBounty
                    )
                )
            }

            return items
        }
    }
}
