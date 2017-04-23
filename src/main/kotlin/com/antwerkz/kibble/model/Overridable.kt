package com.antwerkz.kibble.model

import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.psiUtil.allChildren

/**
 * Marks a type as overridable
 *
 * @property overriding true if this property is overriding a property in a parent type
 */
interface Overridable {
    companion object {
        internal fun apply(kt: KtModifierListOwner): Boolean {
            return kt.modifierList?.allChildren?.find { it.text == "override" } != null
        }
    }

    var overriding: Boolean

    /**
     * @return true if this type overrides something from a parent type
     */
    fun isOverride(): Boolean {
        return overriding
    }
}