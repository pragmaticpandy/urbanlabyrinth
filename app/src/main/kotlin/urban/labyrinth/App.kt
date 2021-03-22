package urban.labyrinth

import urban.labyrinth.CornerCardinality.*

// east to west
val verticalStreets = listOf(Street("15th"), Street("16th"), Street("17th"), Street("18th"))

// north to south
val horizontalStreets = listOf(Street("Denny"), Street("Howell"), Street("Olive"))

val startingCorner = CardinalCorner(SOUTHEAST, Corner(Street("17th"), Street("Howell")))

val numVerticalSegments = verticalStreets.size * 2 * (horizontalStreets.size - 1)
val numHorizontalSegments = horizontalStreets.size * 2 * (verticalStreets.size - 1)
val numSegments = numVerticalSegments + numHorizontalSegments
val numSegmentsTolerance = 5
val maxNumSegments = numSegments + numSegmentsTolerance
val minNumSegments = numSegments - numSegmentsTolerance

data class Street(val name: String) {

    companion object {
        private val northStreets: MutableMap<Street, Street?> = mutableMapOf()
        private val eastStreets: MutableMap<Street, Street?> = mutableMapOf()
        private val southStreets: MutableMap<Street, Street?> = mutableMapOf()
        private val westStreets: MutableMap<Street, Street?> = mutableMapOf()
    }

    // Can only be retrieved for horizontal streets.
    val streetToNorth: Street?
        get() {
            return northStreets.get(this) ?: run {
                val indexOfStreet = horizontalStreets.indexOf(this)
                if (indexOfStreet == -1) {
                    throw Exception("streetToNorth called on street not in horizontalStreets")
                }

                val result = if (indexOfStreet == 0) null else horizontalStreets[indexOfStreet - 1]
                northStreets.put(this, result)
                result
            }
        }

    // Can only be retrieved for vertical streets.
    val streetToEast: Street?
        get() {
            return eastStreets.get(this) ?: run {
                val indexOfStreet = verticalStreets.indexOf(this)
                if (indexOfStreet == -1) {
                    throw Exception("streetToEast called on street not in verticalStreets")
                }

                val result = if (indexOfStreet == 0) null else verticalStreets[indexOfStreet - 1]
                eastStreets.put(this, result)
                result
            }
        }

    // Can only be retrieved for horizontal streets.
    val streetToSouth: Street?
        get() {
            return southStreets.get(this) ?: run {
                val indexOfStreet = horizontalStreets.indexOf(this)
                if (indexOfStreet == -1) {
                    throw Exception("streetToSouth called on street not in horizontalStreets")
                }

                val result = if (indexOfStreet + 1 == horizontalStreets.size) null
                                else horizontalStreets[indexOfStreet + 1]

                southStreets.put(this, result)
                result
            }
        }

    // Can only be retrieved for vertical streets.
    val streetToWest: Street?
        get() {
            return westStreets.get(this) ?: run {
                val indexOfStreet = verticalStreets.indexOf(this)
                if (indexOfStreet == -1) {
                    throw Exception("streetToWest called on street not in verticalStreets")
                }

                val result = if (indexOfStreet + 1 == verticalStreets.size) null
                                else verticalStreets[indexOfStreet + 1]

                westStreets.put(this, result)
                result
            }
        }
}

enum class CornerCardinality { NORTHWEST, NORTHEAST, SOUTHEAST, SOUTHWEST }

data class Corner(val verticalStreet: Street, val horizontalStreet: Street) {

    companion object {
        private val segmentsFromByCorner: MutableMap<Corner, Set<Segment>> = mutableMapOf()
    }

    /**
     * Returns the segments from this corner. Independent of cardinality.
     */
    val segmentsFrom: Set<Segment>
        get() {
            return segmentsFromByCorner.get(this) ?: run {
                val result: MutableSet<Segment> = mutableSetOf()
                horizontalStreet.streetToNorth?.let {
                    result
                        .add(
                            Segment(
                                CardinalCorner(NORTHWEST, this),
                                CardinalCorner(SOUTHWEST, Corner(verticalStreet, it))))

                    result
                        .add(
                            Segment(
                                CardinalCorner(NORTHEAST, this),
                                CardinalCorner(SOUTHEAST, Corner(verticalStreet, it))))
                }

                verticalStreet.streetToEast?.let {
                    result
                        .add(
                            Segment(
                                CardinalCorner(NORTHEAST, this),
                                CardinalCorner(NORTHWEST, Corner(it, horizontalStreet))))

                    result
                        .add(
                            Segment(
                                CardinalCorner(SOUTHEAST, this),
                                CardinalCorner(SOUTHWEST, Corner(it, horizontalStreet))))
                }

                horizontalStreet.streetToSouth?.let {
                    result
                        .add(
                            Segment(
                                CardinalCorner(SOUTHEAST, this),
                                CardinalCorner(NORTHEAST, Corner(verticalStreet, it))))

                    result
                        .add(
                            Segment(
                                CardinalCorner(SOUTHWEST, this),
                                CardinalCorner(NORTHWEST, Corner(verticalStreet, it))))
                }

                verticalStreet.streetToWest?.let {
                    result
                        .add(
                            Segment(
                                CardinalCorner(SOUTHWEST, this),
                                CardinalCorner(SOUTHEAST, Corner(it, horizontalStreet))))

                    result
                        .add(
                            Segment(
                                CardinalCorner(NORTHWEST, this),
                                CardinalCorner(NORTHEAST, Corner(it, horizontalStreet))))
                }

                segmentsFromByCorner.put(this, result)
                result
            }
        }
}

data class CardinalCorner(val cardinality: CornerCardinality, val corner: Corner) {
    val segmentsFrom get() = corner.segmentsFrom
    val verticalStreet get() = corner.verticalStreet
    val horizontalStreet get() = corner.horizontalStreet
}

/**
 * Shouldn't contain any street crossings. Just two corners on same block.
 */
data class Segment(val start: CardinalCorner, val end: CardinalCorner)


fun main() {
    if (!verticalStreets.contains(startingCorner.verticalStreet)) {
        throw Exception("Starting vertical street wasn't in list.")
    }

    if (!horizontalStreets.contains(startingCorner.horizontalStreet)) {
        throw Exception("Starting horizontal street wasn't defined.")
    }

    val paths: ArrayDeque<List<Segment>> = ArrayDeque()
    startingCorner.segmentsFrom.forEach { paths.addLast(listOf(it)) }
    while (paths.size > 0) {
        val path = paths.removeLast()
        if (path.size >= minNumSegments && path.last().end == startingCorner) {
            println(path)
            return
        }

        if (path.size < maxNumSegments) {
            path.last().end.segmentsFrom.forEach { paths.add(path + it) }
        }
    }
}
