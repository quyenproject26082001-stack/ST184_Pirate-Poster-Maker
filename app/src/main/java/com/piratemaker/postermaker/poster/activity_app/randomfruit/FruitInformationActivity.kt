package com.piratemaker.postermaker.poster.activity_app.randomfruit

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.widget.MediaController
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.lvt.ads.util.Admob
import com.piratemaker.postermaker.poster.R
import com.piratemaker.postermaker.poster.activity_app.main.MainActivity
import com.piratemaker.postermaker.poster.core.base.BaseActivity
import com.piratemaker.postermaker.poster.core.extensions.gone
import com.piratemaker.postermaker.poster.core.extensions.handleBackLeftToRight
import com.piratemaker.postermaker.poster.core.extensions.setOnSingleClick
import com.piratemaker.postermaker.poster.core.extensions.showInterAll
import com.piratemaker.postermaker.poster.core.extensions.showToast
import com.piratemaker.postermaker.poster.core.extensions.visible
import com.piratemaker.postermaker.poster.databinding.ActivityFruitInformationBinding
import java.io.File
import java.io.FileOutputStream

class FruitInformationActivity : BaseActivity<ActivityFruitInformationBinding>() {

    private val fruitFolder: String by lazy {
        intent.getStringExtra(EXTRA_FRUIT_FOLDER).orEmpty()
    }

    override fun setViewBinding(): ActivityFruitInformationBinding {
        return ActivityFruitInformationBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        binding.layoutVideo.gone()
        loadFruitContent()
    }

    override fun viewListener() {
        binding.btnBack.setOnSingleClick {
            handleBackLeftToRight()
        }

        binding.btnHome.setOnSingleClick {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish()
        }

        binding.btnEat.setOnSingleClick {
            showInterAll {  playFruitVideo() }
        }

        binding.layoutVideo.setOnSingleClick {
            stopVideo()
        }
    }

    override fun initActionBar() {
    }

    private fun loadFruitContent() {
        if (fruitFolder.isBlank()) {
            showToast(R.string.error)
            return
        }

        val text = readAssetText("fruit/$fruitFolder/text.txt")
        val title = buildTitle(text, fruitFolder)
        val detail = fruitDetails[fruitFolder] ?: buildFallbackDetail(text)

        binding.tvFruitTitle.text = title
        binding.tvFruitType.text = getString(R.string.fruit_type_format, detail.type)
        binding.tvAbout.text = getString(R.string.fruit_about_format, text)
        binding.tvStrengths.text = buildSection(getString(R.string.strengths), detail.strengths)
        binding.tvWeaknesses.text = buildSection(getString(R.string.weaknesses), detail.weaknesses)
        applyStars(buildStarCount(detail.stats.average))
        applyStat(binding.progressAttack, binding.tvAttackValue, detail.stats.attack)
        applyStat(binding.progressDefense, binding.tvDefenseValue, detail.stats.defense)
        applyStat(binding.progressUtility, binding.tvUtilityValue, detail.stats.utility)

        Glide.with(this)
            .load("file:///android_asset/fruit/$fruitFolder/img.webp")
            .placeholder(R.drawable.img_fruit)
            .error(R.drawable.img_fruit)
            .into(binding.imgFruit)
    }

    private fun readAssetText(path: String): String {
        return runCatching {
            assets.open(path).bufferedReader().use { it.readText().trim() }
        }.getOrDefault(getString(R.string.fruit_information_fallback))
    }

    private fun buildTitle(text: String, folder: String): String {
        val firstSentence = text.substringBefore(".").trim()
        val name = Regex("^The\\s+(.+?)\\s+is\\s+", RegexOption.IGNORE_CASE)
            .find(firstSentence)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()

        return name?.takeIf { it.isNotBlank() } ?: getString(R.string.fruit_number, folder)
    }

    private fun buildFruitType(text: String): String {
        return when {
            text.contains("Mythical Zoan", true) -> "Mythical Zoan"
            text.contains("special Paramecia", true) -> "Special Paramecia"
            text.contains("Logia-type", true) -> "Logia"
            text.contains("Zoan-type", true) -> "Zoan"
            text.contains("Paramecia-type", true) -> "Paramecia"
            else -> "Unknown"
        }
    }

    private fun buildSection(title: String, items: List<String>): String {
        return buildString {
            append(title)
            items.forEach {
                append("\n• ")
                append(it)
            }
        }
    }

    private fun buildFallbackDetail(text: String): FruitDetail {
        val type = buildFruitType(text)
        val typeWeakness = when (type) {
            "Logia" -> "Can be pressured by Haki, counters, and bad terrain."
            "Zoan", "Mythical Zoan" -> "Transformation power can become predictable without strategy."
            else -> "Needs strong creativity and timing to reach its best value."
        }
        return FruitDetail(
            type = type,
            strengths = listOf(
                "Creates a strong advantage when the user understands the fruit's core power.",
                "Can change the pace of a fight and surprise enemies."
            ),
            weaknesses = listOf(
                typeWeakness,
                "Still has the standard Devil Fruit weakness to sea water and Sea-Prism Stone."
            ),
            stats = FruitStats(70, 70, 70)
        )
    }

    private fun buildStarCount(average: Int): Int {
        return (average / 20).coerceIn(1, 5)
    }

    private fun applyStars(filled: Int) {
        val stars: List<ImageView> = listOf(
            binding.imgStar1,
            binding.imgStar2,
            binding.imgStar3,
            binding.imgStar4,
            binding.imgStar5
        )
        stars.forEachIndexed { index, imageView ->
            imageView.setImageResource(
                if (index < filled) R.drawable.ic_star_selected else R.drawable.ic_star_unselected
            )
        }
    }

    private fun applyStat(
        progressBar: android.widget.ProgressBar,
        valueView: android.widget.TextView,
        value: Int
    ) {
        progressBar.progress = value
        valueView.text = getString(R.string.stat_value_format, value)
    }

    private fun playFruitVideo() {
        val videoFile = copyVideoToCache() ?: return
        binding.layoutVideo.visible()
        binding.videoFruit.apply {
            setMediaController(MediaController(this@FruitInformationActivity).also {
                it.setAnchorView(this)
            })
            setVideoURI(Uri.fromFile(videoFile))
            setOnPreparedListener { player ->
                player.isLooping = false
                start()
            }
            setOnCompletionListener {
                stopVideo()
            }
        }
    }

    private fun copyVideoToCache(): File? {
        if (fruitFolder.isBlank()) return null

        val outFile = File(cacheDir, "fruit_${fruitFolder}_video.mp4")
        return runCatching {
            assets.open("fruit/$fruitFolder/video.mp4").use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
            outFile
        }.getOrElse {
            showToast(R.string.error)
            null
        }
    }

    private fun stopVideo() {
        binding.videoFruit.stopPlayback()
        binding.layoutVideo.gone()
    }

    override fun onPause() {
        stopVideo()
        super.onPause()
    }

    companion object {
        const val EXTRA_FRUIT_FOLDER = "extra_fruit_folder"

        private val fruitDetails = mapOf(
            "1" to FruitDetail(
                "Mythical Zoan",
                listOf(
                    "Extreme rubber freedom gives elite mobility, defense, and creative attacks.",
                    "Awakened Nika power can bend the fight's rhythm and overwhelm stronger enemies."
                ),
                listOf(
                    "Requires huge stamina and mastery to use at full power.",
                    "Sharp Haki users and sea-based weaknesses remain dangerous."
                ),
                FruitStats(98, 92, 95)
            ),
            "2" to FruitDetail(
                "Paramecia",
                listOf(
                    "Ghosts can pass through defenses and break enemy morale.",
                    "Negative Hollow effects are strong for control and escape."
                ),
                listOf(
                    "Direct damage is limited without setup.",
                    "Confident or unusual enemies can reduce its psychological impact."
                ),
                FruitStats(72, 64, 90)
            ),
            "3" to FruitDetail(
                "Zoan",
                listOf(
                    "Bison form gives strong charge power and physical durability.",
                    "Hybrid form improves close-range pressure."
                ),
                listOf(
                    "Limited ranged options.",
                    "Large transformations are easier to read and counter."
                ),
                FruitStats(78, 85, 55)
            ),
            "4" to FruitDetail(
                "Paramecia",
                listOf(
                    "Can petrify targets affected by attraction or emotion.",
                    "Works well at range and can disable groups quickly."
                ),
                listOf(
                    "Effect depends on target reaction and emotional opening.",
                    "Less reliable against disciplined or immune opponents."
                ),
                FruitStats(82, 70, 88)
            ),
            "5" to FruitDetail(
                "Mythical Zoan",
                listOf(
                    "Dragon form brings massive destructive power, flight, and elemental attacks.",
                    "Hybrid form combines monster durability with elite offense."
                ),
                listOf(
                    "Huge form is a large target.",
                    "Consumes major stamina and still loses to superior Haki or strategy."
                ),
                FruitStats(100, 98, 90)
            ),
            "6" to FruitDetail(
                "Paramecia",
                listOf(
                    "Blade body creates deadly close-combat offense.",
                    "Steel-like body improves resistance to ordinary attacks."
                ),
                listOf(
                    "Range and utility are limited.",
                    "Strong blunt force, Haki, or precision attacks can bypass the advantage."
                ),
                FruitStats(88, 82, 55)
            ),
            "7" to FruitDetail(
                "Paramecia",
                listOf(
                    "Can sprout limbs on surfaces and enemies for instant control.",
                    "Excellent utility for restraint, scouting, and multi-angle attacks."
                ),
                listOf(
                    "Damage to sprouted limbs can transfer back to the user.",
                    "Needs visibility or knowledge of target positions for best results."
                ),
                FruitStats(76, 65, 96)
            ),
            "8" to FruitDetail(
                "Paramecia",
                listOf(
                    "Wax constructs can restrain, shield, and create weapons.",
                    "Very useful for traps and battlefield control."
                ),
                listOf(
                    "Heat and fire can melt the wax.",
                    "Setup time is needed for larger constructs."
                ),
                FruitStats(70, 78, 82)
            ),
            "9" to FruitDetail(
                "Paramecia",
                listOf(
                    "Can eat and assimilate objects for unpredictable upgrades.",
                    "Strong utility for crafting, storage, and transformation."
                ),
                listOf(
                    "Power depends on available materials.",
                    "Close contact is often needed to consume targets or objects."
                ),
                FruitStats(75, 76, 84)
            ),
            "10" to FruitDetail(
                "Zoan",
                listOf(
                    "Giraffe form adds reach, weight, and unusual attack angles.",
                    "Hybrid form increases physical strength and durability."
                ),
                listOf(
                    "Awkward body shape can be exploited.",
                    "Mostly physical, so it depends heavily on combat skill."
                ),
                FruitStats(74, 82, 58)
            ),
            "11" to FruitDetail(
                "Logia",
                listOf(
                    "Smoke body gives intangibility against non-Haki attacks.",
                    "Can capture, obscure vision, and move through the battlefield flexibly."
                ),
                listOf(
                    "Lower raw damage than many Logia fruits.",
                    "Haki and strong wind-based counters are dangerous."
                ),
                FruitStats(78, 86, 83)
            ),
            "12" to FruitDetail(
                "Logia",
                listOf(
                    "Sand control can drain moisture and destroy terrain.",
                    "Excellent area control with Logia intangibility."
                ),
                listOf(
                    "Water and wet conditions heavily weaken the sand body.",
                    "Haki users can still land decisive hits."
                ),
                FruitStats(92, 84, 88)
            ),
            "13" to FruitDetail(
                "Logia",
                listOf(
                    "Fire attacks bring huge destructive power and range.",
                    "Logia body avoids most ordinary physical attacks."
                ),
                listOf(
                    "Can be overpowered by stronger elements or superior Haki.",
                    "Collateral damage makes control difficult in tight spaces."
                ),
                FruitStats(96, 78, 84)
            ),
            "14" to FruitDetail(
                "Paramecia",
                listOf(
                    "Slippery body lets many attacks slide away.",
                    "Great defensive value and movement utility."
                ),
                listOf(
                    "Lower direct offensive power.",
                    "Haki, restraint, or environmental traps can still stop the user."
                ),
                FruitStats(60, 88, 70)
            ),
            "15" to FruitDetail(
                "Paramecia",
                listOf(
                    "Strings offer cutting, restraint, mobility, puppetry, and repair tricks.",
                    "Awakening gives outstanding battlefield control."
                ),
                listOf(
                    "Requires high skill and multitasking.",
                    "Strong Haki or overwhelming force can break through strings."
                ),
                FruitStats(95, 82, 93)
            ),
            "16" to FruitDetail(
                "Paramecia",
                listOf(
                    "Ink drawings can become living constructs for scouting, attacks, and deception.",
                    "High creativity gives huge utility."
                ),
                listOf(
                    "Power depends on drawing speed and quality.",
                    "Water, interruption, or poor preparation can limit output."
                ),
                FruitStats(70, 60, 98)
            ),
            "17" to FruitDetail(
                "Paramecia",
                listOf(
                    "Magnetism controls metal for crushing attacks, armor, and weapons.",
                    "Excellent against armed enemies and metal-rich areas."
                ),
                listOf(
                    "Less effective where metal is scarce.",
                    "Precision control can be disrupted by speed or stronger Haki."
                ),
                FruitStats(94, 83, 91)
            ),
            "18" to FruitDetail(
                "Paramecia",
                listOf(
                    "Shadow control can steal shadows and create powerful servants.",
                    "Strong control and army-building potential."
                ),
                listOf(
                    "Sunlight rules create major risk for stolen shadows.",
                    "Requires setup and management of shadows."
                ),
                FruitStats(91, 80, 96)
            ),
            "19" to FruitDetail(
                "Paramecia",
                listOf(
                    "Paw repulsion can deflect attacks, move people, and compress air into devastating force.",
                    "One of the best mobility and support powers."
                ),
                listOf(
                    "Palm contact and timing are important.",
                    "Sea water, Sea-Prism Stone, and elite Haki remain threats."
                ),
                FruitStats(98, 90, 95)
            ),
            "20" to FruitDetail(
                "Zoan",
                listOf(
                    "Grants human intelligence and transformation utility to an animal user.",
                    "Hybrid forms can support medicine, movement, and combat."
                ),
                listOf(
                    "Combat ceiling depends on training and forms.",
                    "Less naturally destructive than most combat fruits."
                ),
                FruitStats(65, 70, 78)
            ),
            "21" to FruitDetail(
                "Paramecia",
                listOf(
                    "ROOM allows surgical control over space, bodies, and objects.",
                    "Elite utility for teleportation, disassembly, support, and offense."
                ),
                listOf(
                    "Consumes heavy stamina.",
                    "Needs concentration and a created operating area."
                ),
                FruitStats(97, 86, 100)
            ),
            "22" to FruitDetail(
                "Special Paramecia",
                listOf(
                    "Mochi body can reshape, trap, and strike like a Logia-style body.",
                    "Excellent synergy with observation Haki."
                ),
                listOf(
                    "Moisture and eating can reduce stickiness.",
                    "Needs high control to maintain advanced forms."
                ),
                FruitStats(92, 88, 86)
            ),
            "23" to FruitDetail(
                "Zoan",
                listOf(
                    "Leopard form gives speed, claws, fangs, and lethal close combat.",
                    "Hybrid form greatly boosts assassination-style offense."
                ),
                listOf(
                    "Mostly close-range and physical.",
                    "Can be countered by stronger Haki or ranged control."
                ),
                FruitStats(88, 82, 62)
            ),
            "24" to FruitDetail(
                "Paramecia",
                listOf(
                    "Singing can pull minds into a virtual space.",
                    "Extremely strong crowd control and support potential."
                ),
                listOf(
                    "Depends on voice, sound delivery, and target exposure.",
                    "Physical body can be vulnerable while power is active."
                ),
                FruitStats(70, 55, 97)
            ),
            "25" to FruitDetail(
                "Paramecia",
                listOf(
                    "Can stitch living things and objects together with strong control utility.",
                    "Great for restraint, repair, and tactical support."
                ),
                listOf(
                    "Needs thread placement and contact opportunities.",
                    "Lower raw damage compared with destructive fruits."
                ),
                FruitStats(62, 72, 89)
            ),
            "26" to FruitDetail(
                "Mythical Zoan",
                listOf(
                    "Eight-headed serpent form is difficult to finish quickly.",
                    "Multiple heads create intimidation and survival value."
                ),
                listOf(
                    "Large body can be targeted from many angles.",
                    "Effectiveness depends heavily on user combat skill."
                ),
                FruitStats(86, 92, 70)
            ),
            "27" to FruitDetail(
                "Paramecia",
                listOf(
                    "Touch can turn people into toys and erase memories of them.",
                    "One successful contact can decide a battle instantly."
                ),
                listOf("Requires direct touch.", "User's defeat can undo the effect."),
                FruitStats(68, 60, 100)
            ),
            "28" to FruitDetail(
                "Paramecia",
                listOf(
                    "Can disassemble and rebuild objects into massive weapons.",
                    "Very dangerous in environments full of machinery or debris."
                ),
                listOf(
                    "Depends on available materials.",
                    "Large constructs can be slow and exposed to precise counters."
                ),
                FruitStats(95, 85, 88)
            ),
            "29" to FruitDetail(
                "Paramecia",
                listOf(
                    "Weight changes enable crushing drops and evasive light movement.",
                    "Simple power with strong mobility and surprise attacks."
                ),
                listOf(
                    "Needs vertical space or momentum for best damage.",
                    "Limited defense and utility outside weight control."
                ),
                FruitStats(70, 58, 74)
            ),
            "30" to FruitDetail(
                "Paramecia",
                listOf(
                    "Can create phantom images from touched pictures.",
                    "Strong deception, misdirection, and tactical confusion."
                ),
                listOf(
                    "Relies on available images and setup.",
                    "Illusions can fail if the enemy identifies the trick."
                ),
                FruitStats(64, 55, 94)
            )
        )
    }

    private data class FruitDetail(
        val type: String,
        val strengths: List<String>,
        val weaknesses: List<String>,
        val stats: FruitStats
    )

    private data class FruitStats(
        val attack: Int,
        val defense: Int,
        val utility: Int
    ) {
        val average: Int = (attack + defense + utility) / 3
    }

    override fun onRestart() {
        super.onRestart()
        initAds()
    }

    //quyen
    override fun initAds() {
        // Load native regular ad above back button and list

        // Load native collapsible ad at bottom
        Admob.getInstance().loadNativeCollapNotBanner(
            this,
            getString(R.string.native_collap_fruitInfo),
            binding.nativeCollapFruitInformation
        )
    }


}
