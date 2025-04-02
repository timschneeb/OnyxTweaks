package me.timschneeberger.onyxtweaks.ui.editor.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.widget.Button
import android.widget.LinearLayout
import com.amrdeveloper.codeview.CodeView

class SymbolInputView : LinearLayout {
    private var editor: CodeView? = null

    @JvmOverloads
    constructor(
        context: Context?,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        orientation = HORIZONTAL
    }

    fun bindEditor(editor: CodeView?) {
        this.editor = editor
    }

    /**
     * Add symbols to the view.
     *
     * @param display    The texts displayed in button
     * @param insertText The actual text to be inserted to editor when the button is clicked
     */
    fun addSymbols(display: Array<String?>, insertText: Array<String?>) {
        val count = display.size.coerceAtLeast(insertText.size)
        for (i in 0 until count) {
            val btn = Button(context, null, android.R.attr.buttonStyleSmall)
            btn.text = display[i]
            btn.background = ColorDrawable(Color.TRANSPARENT)
            addView(btn, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
            btn.setOnClickListener {
                editor?.let {
                    it.text.insert(it.selectionStart, insertText[i])
                }
            }
        }
    }

    fun forEachButton(consumer: ButtonConsumer) {
        for (i in 0 until childCount) {
            consumer.accept(getChildAt(i) as Button)
        }
    }

    interface ButtonConsumer {
        fun accept(btn: Button)
    }
}