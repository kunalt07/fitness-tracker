package com.example.fitness_tracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Minimal Markdown renderer covering what the AI plan actually emits:
 * '#' / '##' / '###' headers, '-' / '*' / '•' bullets, '1.' ordered items,
 * inline **bold** and *italic*. Anything else falls through as a paragraph.
 */
@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val blocks = remember(text) { parseBlocks(text) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        blocks.forEach { block -> RenderBlock(block) }
    }
}

@Composable
private fun RenderBlock(block: Block) {
    when (block) {
        is Block.Heading -> Text(
            text = renderInline(block.text),
            style = when (block.level) {
                1 -> MaterialTheme.typography.headlineSmall
                2 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            },
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp),
        )
        is Block.Paragraph -> Text(
            text = renderInline(block.text),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        is Block.Bullet -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = block.marker,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = renderInline(block.text),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private sealed interface Block {
    data class Heading(val level: Int, val text: String) : Block
    data class Paragraph(val text: String) : Block
    data class Bullet(val marker: String, val text: String) : Block
}

private val HEADING = Regex("^\\s*(#{1,6})\\s+(.*)$")
private val BULLET = Regex("^\\s*([-*•])\\s+(.*)$")
private val ORDERED = Regex("^\\s*(\\d+[.)])\\s+(.*)$")

private fun parseBlocks(text: String): List<Block> {
    val out = mutableListOf<Block>()
    val paragraphLines = mutableListOf<String>()

    fun flushParagraph() {
        if (paragraphLines.isNotEmpty()) {
            out += Block.Paragraph(paragraphLines.joinToString(" "))
            paragraphLines.clear()
        }
    }

    text.lines().forEach { raw ->
        val line = raw.trimEnd()
        if (line.isBlank()) {
            flushParagraph()
            return@forEach
        }
        HEADING.matchEntire(line)?.let { m ->
            flushParagraph()
            out += Block.Heading(level = m.groupValues[1].length, text = m.groupValues[2].trim())
            return@forEach
        }
        BULLET.matchEntire(line)?.let { m ->
            flushParagraph()
            out += Block.Bullet(marker = "•", text = m.groupValues[2].trim())
            return@forEach
        }
        ORDERED.matchEntire(line)?.let { m ->
            flushParagraph()
            out += Block.Bullet(marker = m.groupValues[1], text = m.groupValues[2].trim())
            return@forEach
        }
        paragraphLines += line.trimStart()
    }
    flushParagraph()
    return out
}

/** Inline pass: **bold** then *italic*; a final cleanup strips stray underscores/backticks. */
private fun renderInline(s: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < s.length) {
        val rest = s.substring(i)
        // **bold**
        val bold = BOLD.find(rest)
        val italic = ITALIC.find(rest)
        val pick = listOfNotNull(bold, italic)
            .filter { it.range.first == 0 || it.range.first <= (bold?.range?.first ?: Int.MAX_VALUE) }
            .minByOrNull { it.range.first }
        if (pick == null) {
            append(rest)
            break
        }
        if (pick.range.first > 0) {
            append(rest.substring(0, pick.range.first))
        }
        when {
            pick == bold -> withStyleSafe(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append(pick.groupValues[1])
            }
            pick == italic -> withStyleSafe(SpanStyle(fontStyle = FontStyle.Italic)) {
                append(pick.groupValues[1])
            }
        }
        i += pick.range.last + 1
    }
}

private inline fun androidx.compose.ui.text.AnnotatedString.Builder.withStyleSafe(
    style: SpanStyle,
    block: androidx.compose.ui.text.AnnotatedString.Builder.() -> Unit,
) {
    val mark = pushStyle(style)
    try {
        block()
    } finally {
        pop(mark)
    }
}

private val BOLD = Regex("\\*\\*([^*]+)\\*\\*")
private val ITALIC = Regex("\\*([^*]+)\\*")
