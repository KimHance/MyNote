package com.hancekim.mynote.core.designsystem.block

/**
 * 표 열마다 글자 자리 폭을 고른다. 다 들어가면 목표 폭, 최소 폭은 들어가면 남는 폭을 (목표 − 최소) 비율로 나누고,
 * 그마저 넘치면 최소 폭으로 두어 가로로 스크롤한다.
 *
 * @param longestLine 열마다 가장 긴 한 줄 폭
 * @param longestWord 열마다 가장 긴 낱말 폭
 * @param available 열 글자 자리에 쓸 수 있는 폭의 합
 * @param targetCap 목표 폭 상한
 * @param minimumFloor 최소 폭 하한
 */
internal fun tableColumnWidths(
    longestLine: IntArray,
    longestWord: IntArray,
    available: Int,
    targetCap: Int,
    minimumFloor: Int,
): IntArray {
    val target = IntArray(longestLine.size) { minOf(longestLine[it], targetCap) }
    val minimum = IntArray(target.size) { minOf(target[it], maxOf(longestWord[it], minimumFloor)) }
    val targetSum = target.sum()
    val minimumSum = minimum.sum()
    return when {
        targetSum <= available -> target
        minimumSum <= available -> spread(minimum, target, available - minimumSum)
        else -> minimum
    }
}

private fun spread(
    minimum: IntArray,
    target: IntArray,
    extra: Int,
): IntArray {
    val room = LongArray(minimum.size) { (target[it] - minimum[it]).toLong() }
    val roomSum = room.sum()
    val widths = IntArray(minimum.size) { minimum[it] + (extra * room[it] / roomSum).toInt() }
    var left = minimum.sum() + extra - widths.sum()
    var column = 0
    while (left > 0) {
        if (widths[column] < target[column]) {
            widths[column]++
            left--
        }
        column = (column + 1) % widths.size
    }
    return widths
}
