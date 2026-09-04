package com.example.model

import org.json.JSONArray
import org.json.JSONObject

object DocumentSerializer {

    fun serialize(doc: Document): String {
        val root = JSONObject()
        root.put("id", doc.id)
        root.put("title", doc.title)
        root.put("format", doc.format.name)
        root.put("lastSavedMillis", doc.lastSavedMillis)

        val blocksArray = JSONArray()
        for (block in doc.blocks) {
            val blockObj = JSONObject()
            when (block) {
                is ParagraphBlock -> {
                    blockObj.put("type", "paragraph")
                    blockObj.put("id", block.id)
                    blockObj.put("text", block.text)
                    blockObj.put("headingLevel", block.headingLevel.name)
                    blockObj.put("alignment", block.alignment.name)
                    blockObj.put("listType", block.listType.name)
                    blockObj.put("isChecked", block.isChecked)
                    blockObj.put("indentLevel", block.indentLevel)
                    blockObj.put("isBold", block.isBold)
                    blockObj.put("isItalic", block.isItalic)
                    blockObj.put("isUnderline", block.isUnderline)
                    blockObj.put("isStrike", block.isStrike)
                    blockObj.put("textColorHex", block.textColorHex)
                    if (block.highlightColorHex != null) {
                        blockObj.put("highlightColorHex", block.highlightColorHex)
                    }
                    blockObj.put("fontSizeSp", block.fontSizeSp.toDouble())
                    blockObj.put("fontFamily", block.fontFamily.name)
                    blockObj.put("lineSpacingMultiplier", block.lineSpacingMultiplier.toDouble())
                }
                is TableBlock -> {
                    blockObj.put("type", "table")
                    blockObj.put("id", block.id)
                    blockObj.put("rows", block.rows)
                    blockObj.put("cols", block.cols)
                    val rowsArray = JSONArray()
                    for (row in block.cells) {
                        val rowArray = JSONArray()
                        for (cell in row) {
                            rowArray.put(cell)
                        }
                        rowsArray.put(rowArray)
                    }
                    blockObj.put("cells", rowsArray)
                }
                is ImageBlock -> {
                    blockObj.put("type", "image")
                    blockObj.put("id", block.id)
                    blockObj.put("uriOrData", block.uriOrData)
                    blockObj.put("caption", block.caption)
                    blockObj.put("widthPercent", block.widthPercent.toDouble())
                    blockObj.put("alignment", block.alignment.name)
                }
                is HorizontalRuleBlock -> {
                    blockObj.put("type", "horizontal_rule")
                    blockObj.put("id", block.id)
                }
                is PageBreakBlock -> {
                    blockObj.put("type", "page_break")
                    blockObj.put("id", block.id)
                }
            }
            blocksArray.put(blockObj)
        }
        root.put("blocks", blocksArray)
        return root.toString()
    }

    fun deserialize(jsonStr: String): Document {
        if (jsonStr.isBlank()) return Document()
        return try {
            val root = JSONObject(jsonStr)
            val id = root.optLong("id", 0L)
            val title = root.optString("title", "Untitled Document")
            val formatStr = root.optString("format", ExportFormat.DOCX.name)
            val format = try { ExportFormat.valueOf(formatStr) } catch (e: Exception) { ExportFormat.DOCX }
            val lastSaved = root.optLong("lastSavedMillis", System.currentTimeMillis())

            val blocksList = mutableListOf<DocBlock>()
            val blocksArray = root.optJSONArray("blocks")
            if (blocksArray != null) {
                for (i in 0 until blocksArray.length()) {
                    val obj = blocksArray.optJSONObject(i) ?: continue
                    val type = obj.optString("type")
                    val blockId = obj.optString("id", java.util.UUID.randomUUID().toString())
                    when (type) {
                        "paragraph" -> {
                            val heading = try {
                                HeadingLevel.valueOf(obj.optString("headingLevel", "NORMAL"))
                            } catch (e: Exception) { HeadingLevel.NORMAL }
                            val alignment = try {
                                DocAlignment.valueOf(obj.optString("alignment", "LEFT"))
                            } catch (e: Exception) { DocAlignment.LEFT }
                            val listType = try {
                                DocListType.valueOf(obj.optString("listType", "NONE"))
                            } catch (e: Exception) { DocListType.NONE }
                            val fontFamily = try {
                                DocFontFamily.valueOf(obj.optString("fontFamily", "SANS_SERIF"))
                            } catch (e: Exception) { DocFontFamily.SANS_SERIF }

                            blocksList.add(
                                ParagraphBlock(
                                    id = blockId,
                                    text = obj.optString("text", ""),
                                    headingLevel = heading,
                                    alignment = alignment,
                                    listType = listType,
                                    isChecked = obj.optBoolean("isChecked", false),
                                    indentLevel = obj.optInt("indentLevel", 0),
                                    isBold = obj.optBoolean("isBold", false),
                                    isItalic = obj.optBoolean("isItalic", false),
                                    isUnderline = obj.optBoolean("isUnderline", false),
                                    isStrike = obj.optBoolean("isStrike", false),
                                    textColorHex = obj.optString("textColorHex", "#1A1A1A"),
                                    highlightColorHex = if (obj.has("highlightColorHex")) obj.getString("highlightColorHex") else null,
                                    fontSizeSp = obj.optDouble("fontSizeSp", 15.0).toFloat(),
                                    fontFamily = fontFamily,
                                    lineSpacingMultiplier = obj.optDouble("lineSpacingMultiplier", 1.15).toFloat()
                                )
                            )
                        }
                        "table" -> {
                            val rows = obj.optInt("rows", 3)
                            val cols = obj.optInt("cols", 3)
                            val cellsList = mutableListOf<List<String>>()
                            val cellsArray = obj.optJSONArray("cells")
                            if (cellsArray != null) {
                                for (r in 0 until cellsArray.length()) {
                                    val rowArr = cellsArray.optJSONArray(r)
                                    val rowList = mutableListOf<String>()
                                    if (rowArr != null) {
                                        for (c in 0 until rowArr.length()) {
                                            rowList.add(rowArr.optString(c, ""))
                                        }
                                    }
                                    cellsList.add(rowList)
                                }
                            }
                            blocksList.add(TableBlock(id = blockId, rows = rows, cols = cols, cells = cellsList))
                        }
                        "image" -> {
                            val alignment = try {
                                DocAlignment.valueOf(obj.optString("alignment", "CENTER"))
                            } catch (e: Exception) { DocAlignment.CENTER }
                            blocksList.add(
                                ImageBlock(
                                    id = blockId,
                                    uriOrData = obj.optString("uriOrData", ""),
                                    caption = obj.optString("caption", ""),
                                    widthPercent = obj.optDouble("widthPercent", 0.8).toFloat(),
                                    alignment = alignment
                                )
                            )
                        }
                        "horizontal_rule" -> {
                            blocksList.add(HorizontalRuleBlock(id = blockId))
                        }
                        "page_break" -> {
                            blocksList.add(PageBreakBlock(id = blockId))
                        }
                    }
                }
            }

            Document(
                id = id,
                title = title,
                format = format,
                blocks = blocksList,
                isModified = false,
                lastSavedMillis = lastSaved
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Document()
        }
    }
}
