package com.jeremy.lumi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class LocalizationIntegrityTest {

    private fun loadStrings(file: File): Pair<Map<String, String>, Map<String, List<String>>> {
        assertTrue("Resource file does not exist: ${file.absolutePath}", file.exists())
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(file)
        doc.documentElement.normalize()

        val strings = mutableMapOf<String, String>()
        val stringNodes = doc.getElementsByTagName("string")
        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i) as Element
            val name = node.getAttribute("name")
            strings[name] = node.textContent
        }

        val arrays = mutableMapOf<String, List<String>>()
        val arrayNodes = doc.getElementsByTagName("string-array")
        for (i in 0 until arrayNodes.length) {
            val node = arrayNodes.item(i) as Element
            val name = node.getAttribute("name")
            val itemNodes = node.getElementsByTagName("item")
            val items = mutableListOf<String>()
            for (j in 0 until itemNodes.length) {
                items.add(itemNodes.item(j).textContent)
            }
            arrays[name] = items
        }

        return Pair(strings, arrays)
    }

    private fun extractSpecifiers(text: String): List<String> {
        val regex = Regex("""%(\d+\$)?[.\d]*[sdfx%]""")
        return regex.findAll(text).map { it.value }.sorted().toList()
    }

    @Test
    fun testStringKeyParityAndSpecifiers() {
        val resourceFiles = listOf("strings.xml", "strings_chat.xml", "strings_audit.xml")

        for (fileName in resourceFiles) {
            val esFile = File("src/main/res/values/$fileName").takeIf { it.exists() }
                ?: File("app/src/main/res/values/$fileName")
            val enFile = File("src/main/res/values-en/$fileName").takeIf { it.exists() }
                ?: File("app/src/main/res/values-en/$fileName")

            val (esStrings, esArrays) = loadStrings(esFile)
            val (enStrings, enArrays) = loadStrings(enFile)

            // 1. Strings parity
            val missingInEn = esStrings.keys - enStrings.keys
            assertTrue("English $fileName is missing keys: $missingInEn", missingInEn.isEmpty())

            val extraInEn = enStrings.keys - esStrings.keys
            assertTrue("English $fileName has extra unexpected keys: $extraInEn", extraInEn.isEmpty())

            // 2. Format specifier integrity for strings
            for ((name, esValue) in esStrings) {
                val enValue = enStrings[name] ?: continue
                val esSpecifiers = extractSpecifiers(esValue)
                val enSpecifiers = extractSpecifiers(enValue)
                assertEquals(
                    "Format specifier mismatch in string '$name' ($fileName). ES: $esSpecifiers vs EN: $enSpecifiers",
                    esSpecifiers,
                    enSpecifiers
                )
            }

            // 3. String-arrays parity and item counts
            val missingArraysInEn = esArrays.keys - enArrays.keys
            assertTrue("English $fileName is missing string-arrays: $missingArraysInEn", missingArraysInEn.isEmpty())

            for ((name, esItems) in esArrays) {
                val enItems = enArrays[name] ?: continue
                assertEquals(
                    "Item count mismatch in array '$name' ($fileName)",
                    esItems.size,
                    enItems.size
                )
                for (i in esItems.indices) {
                    val esSpec = extractSpecifiers(esItems[i])
                    val enSpec = extractSpecifiers(enItems[i])
                    assertEquals(
                        "Format specifier mismatch in array '$name' item $i ($fileName)",
                        esSpec,
                        enSpec
                    )
                }
            }
        }
    }
}
