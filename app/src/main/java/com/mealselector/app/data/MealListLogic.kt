package com.mealselector.app.data

object MealListLogic {

    fun indexAfterRemove(currentIndex: Int, removedIndex: Int, newSize: Int): Int {
        if (newSize == 0) return 0
        return when {
            removedIndex < currentIndex -> (currentIndex - 1).coerceIn(0, newSize - 1)
            removedIndex > currentIndex -> currentIndex.coerceIn(0, newSize - 1)
            else -> currentIndex.coerceAtMost(newSize - 1)
        }
    }

    fun reorder(meals: List<String>, fromIndex: Int, toIndex: Int): List<String> {
        if (fromIndex !in meals.indices || toIndex !in meals.indices || fromIndex == toIndex) {
            return meals
        }
        val updated = meals.toMutableList()
        val item = updated.removeAt(fromIndex)
        updated.add(toIndex, item)
        return updated
    }

    fun indexAfterReorder(reorderedMeals: List<String>, previousMeals: List<String>, previousIndex: Int): Int {
        if (reorderedMeals.isEmpty()) return 0
        val currentMeal = previousMeals.getOrNull(previousIndex) ?: return 0
        val newIndex = reorderedMeals.indexOf(currentMeal)
        return if (newIndex >= 0) {
            newIndex
        } else {
            previousIndex.coerceIn(0, reorderedMeals.lastIndex)
        }
    }
}
