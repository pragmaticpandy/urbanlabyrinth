package urban.labyrinth

import kotlin.math.max
import java.util.UUID
import java.nio.file.Files
import java.nio.file.Paths
import urban.labyrinth.CornerCardinality.*
import urban.labyrinth.CardinalDirection.*
import urban.labyrinth.Turn.*

// west to east
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

                val result = if (indexOfStreet + 1 == verticalStreets.size) null
                                else verticalStreets[indexOfStreet + 1]

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

                val result = if (indexOfStreet == 0) null else verticalStreets[indexOfStreet - 1]
                westStreets.put(this, result)
                result
            }
        }

    override fun toString(): String = name
}

enum class CornerCardinality {
    NORTHWEST, NORTHEAST, SOUTHEAST, SOUTHWEST;

    override fun toString(): String = name.toLowerCase()
}

enum class CardinalDirection {
    NORTH, SOUTH, EAST, WEST;

    override fun toString(): String = name.toLowerCase()
}

enum class Turn { STRAIGHT, LEFT, RIGHT, UTURN }

data class Corner(val verticalStreet: Street, val horizontalStreet: Street) {

    companion object {
        private val segmentsFromByCorner: MutableMap<Corner, Set<Segment>> = mutableMapOf()
    }

    /**
     * Returns the segments from this corner. Independent of cardinality.
     */
    val segmentsFrom: Set<Segment>
        get() {
            return segmentsFromByCorner.get(this) ?.let {
                val result = it.toMutableList()
                result.shuffle() // to get more random results
                result.toSet()
            } ?: run {
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

    private val caddywampusCardinalities: Set<Set<CornerCardinality>> = setOf(
        setOf(NORTHWEST, SOUTHEAST), setOf(NORTHEAST, SOUTHWEST))

    private val verticalScreetCrossingCardinalities: Set<Set<CornerCardinality>> = setOf(
        setOf(NORTHWEST, NORTHEAST), setOf(SOUTHWEST, SOUTHEAST))

    /**
     * For two cardinal corners at same corner. Either "cross both streets", "don't cross", or
     * "cross $streetName"
     */
    fun getCrossText(other: CardinalCorner): Pair<String, Int> {
        if (corner != other.corner) {
            throw Exception("getCrossText shouldn't have been called for different corners.")
        }

        if (cardinality == other.cardinality) {
            return "don't cross" to 0
        }

        if (caddywampusCardinalities.contains(setOf(cardinality, other.cardinality))) {
            return "cross both streets" to 2
        }

        if (verticalScreetCrossingCardinalities.contains(setOf(cardinality, other.cardinality))) {
            return "cross ${corner.verticalStreet}" to 1
        }

        return "cross ${corner.horizontalStreet}" to 1
    }
}

/**
 * Shouldn't contain any street crossings. Just two corners on same block.
 */
data class Segment(val start: CardinalCorner, val end: CardinalCorner) {

    /**
     * Given the start and end cardinal locations relative to the start and end corners, which
     * cardinal direction is traveled?
     */
    private val directions: Map<Pair<CornerCardinality, CornerCardinality>, CardinalDirection> = mapOf(
        Pair(NORTHWEST, SOUTHWEST) to NORTH,
        Pair(NORTHWEST, NORTHEAST) to WEST,
        Pair(NORTHEAST, SOUTHEAST) to NORTH,
        Pair(NORTHEAST, NORTHWEST) to EAST,
        Pair(SOUTHEAST, SOUTHWEST) to EAST,
        Pair(SOUTHEAST, NORTHEAST) to SOUTH,
        Pair(SOUTHWEST, NORTHWEST) to SOUTH,
        Pair(SOUTHWEST, SOUTHEAST) to WEST)

    /**
     * What turn did you just make given a sequence of cardinal directions?
     */
    private val turns: Map<Pair<CardinalDirection, CardinalDirection>, Turn> = mapOf(
        Pair(NORTH, NORTH) to STRAIGHT,
        Pair(NORTH, SOUTH) to UTURN,
        Pair(NORTH, EAST) to RIGHT,
        Pair(NORTH, WEST) to LEFT,
        Pair(SOUTH, NORTH) to UTURN,
        Pair(SOUTH, SOUTH) to STRAIGHT,
        Pair(SOUTH, EAST) to LEFT,
        Pair(SOUTH, WEST) to RIGHT,
        Pair(EAST, NORTH) to LEFT,
        Pair(EAST, SOUTH) to RIGHT,
        Pair(EAST, EAST) to STRAIGHT,
        Pair(EAST, WEST) to UTURN,
        Pair(WEST, NORTH) to RIGHT,
        Pair(WEST, SOUTH) to LEFT,
        Pair(WEST, EAST) to UTURN,
        Pair(WEST, WEST) to STRAIGHT)

    val directionless: Set<CardinalCorner> get() = setOf(start, end)

    /**
     * Street this segment follows
     */
    val street: Street
        get() {
            if (start.horizontalStreet == end.horizontalStreet) {
                return start.horizontalStreet
            } else if (start.verticalStreet == end.verticalStreet) {
                return start.verticalStreet
            } else {
                throw Exception("Invalid segment created where neither street is shared between corners")
            }
        }

    /**
     * Cross street this segment ends at. I.e. at the ending corner, the street that wasn't walked
     * along.
     */
    val endingStreet: Street
        get() {
            return if (end.horizontalStreet == street) end.verticalStreet else end.horizontalStreet
        }

    val direction: CardinalDirection
        get() {
            return directions.get(Pair(start.cardinality, end.cardinality))
                    ?: throw Exception("Invalid segment—couldn't determine heading")
        }

    fun getTurn(segment: Segment): Turn {
        return turns.get(Pair(direction, segment.direction))
                ?: throw Exception("Invalid segment—couldn't determine turn")
    }
}

val outputDir = Paths.get(System.getProperty("user.dir")).resolve("output")

fun main() {
    if (!verticalStreets.contains(startingCorner.verticalStreet)) {
        throw Exception("Starting vertical street wasn't in list.")
    }

    if (!horizontalStreets.contains(startingCorner.horizontalStreet)) {
        throw Exception("Starting horizontal street wasn't defined.")
    }

    Files.createDirectories(outputDir)

    val paths: ArrayDeque<List<Segment>> = ArrayDeque()
    startingCorner.segmentsFrom.forEach { paths.addLast(listOf(it)) }
    while (paths.size > 0) {
        val path = paths.removeLast()
        if (path.size >= minNumSegments && path.last().end == startingCorner) {
            scoreAndDump(path)
        }

        if (path.size < maxNumSegments) {
            path.last().end.segmentsFrom.forEach {

                // if it isn't a uturn, add it to the stack
                if (it.end.corner != path.last().start.corner) paths.add(path + it)
            }
        }
    }
}

var highScore = 0

fun scoreAndDump(path: List<Segment>) {
    val (score, instructions) = score(path)
    highScore = max(highScore, score)
    if (score >= highScore) {
        Files
            .write(
                outputDir.resolve("$score-${UUID.randomUUID()}.txt"),
                instructions + path.map { it.toString() })
    }

    print("\rhigh score: $highScore")
}

fun score(path: List<Segment>): Pair<Int, List<String>> {

    // 10 points for each unique segment
    val numUniqueSegments = path.map { it.directionless }.toSet().size
    var score = numUniqueSegments * 10

    // -9 for each re-used segment
    score -= (path.size - numUniqueSegments) * 9

    // -1 for each street cross
    val (instructions, numCrosses) = getInstructionLines(path)
    score -= numCrosses

    // todo uturns


    return Pair(score, instructions)
}

fun getInstructionLines(path: List<Segment>): Pair<List<String>, Int> {
    var result: List<String> = listOf()
    var totalNumCrosses = 0
    result +=
        """
        Start at the ${path.first().start.cardinality} corner of ${path.first().start.verticalStreet}
        and ${path.first().start.horizontalStreet} and head ${path.first().direction} on
        ${path.first().street.name}
        """.trimIndent().replace('\n', ' ')

    for (i in 0..(path.size - 2)) {
        val segmentA = path[i]
        val segmentB = path[i + 1]

        /**
         * Same cardinality on both segments means it was straight with a single straight cross, so
         * we can skip giving an instruction
         */
        if (segmentA.start.cardinality == segmentB.start.cardinality
            && segmentA.end.cardinality == segmentB.end.cardinality) {

            continue;
        }

        val (crossText, numCrosses) = segmentA.end.getCrossText(segmentB.start)
        totalNumCrosses += numCrosses

        result += "At ${segmentA.endingStreet}, $crossText and ${
                    when (segmentA.getTurn(segmentB)) {
                        STRAIGHT -> "continue straight"
                        LEFT -> "turn left"
                        RIGHT -> "turn right"
                        UTURN -> "head back the way you came"
                        else -> throw Exception("turn wasn't recognized")
                    }}"
    }

    result += "Done."
    return result to totalNumCrosses
}
