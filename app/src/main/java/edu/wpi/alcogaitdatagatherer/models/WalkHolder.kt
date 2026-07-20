package edu.wpi.alcogaitdatagatherer.models

import edu.wpi.alcogaitdatagatherercommon.WalkType
import java.io.Serializable

/**
 * Created by Adonay on 11/30/2017.
 *
 * Groups together all the [Walk] trials recorded for a single walk number
 * (e.g. NORMAL, STANDING_ON_ONE_FOOT, etc., depending on how many walk
 * types were configured). Also tracks which [WalkType]s are currently
 * "allowed" for this walk number, and lets the caller step through them
 * in order.
 */
class WalkHolder(
    // Which walk number (trial set) this holder represents
    val walkNumber: Int
) : Serializable {

    // Maps each walk type recorded so far to its corresponding Walk data
    private val walkMap: HashMap<WalkType, Walk> = HashMap()

    // The ordered set of walk types expected for this walk number
    // Defaults to all WalkType values; can be narrowed via setAllowedWalkTypes()
    private var allowedWalkTypes: Array<WalkType> = WalkType.values()

    /** Returns the recorded Walk for the given type, or null if it hasn't been recorded yet */
    fun get(walkType: WalkType?): Walk? {
        return walkMap[walkType]
    }

    /** Stores a completed Walk under its walk type. Returns this holder for chaining */
    fun addWalk(walk: Walk?): WalkHolder {
        walk?.let { walkMap[it.walkType] = it }
        return this
    }

    /** Removes any recorded Walk for the given type (used when redoing a walk). Returns this holder for chaining */
    fun removeWalk(walkType: WalkType): WalkHolder {
        walkMap.remove(walkType)
        return this
    }

    /**
     * Finds the first walk type (in [allowedWalkTypes] order) that hasn't
     * been recorded yet, or null if every allowed walk type is already done
     */
    val nextWalkType: WalkType?
        get() {
            for (walkType in allowedWalkTypes) {
                if (!walkMap.containsKey(walkType)) {
                    return walkType
                }
            }
            return null
        }

    /**
     * Finds the walk type that comes immediately before [currentWalkType]
     * in [allowedWalkTypes] order. Returns null if [currentWalkType] is the
     * first allowed type; returns the last allowed type if [currentWalkType]
     * isn't found in the list at all
     */
    fun getPreviousWalkType(currentWalkType: WalkType?): WalkType? {
        var prevWalkType: WalkType? = null
        for (walkType in allowedWalkTypes) {
            if (walkType == currentWalkType) {
                return prevWalkType
            }
            prevWalkType = walkType
        }
        return prevWalkType
    }

    /** Whether a Walk has already been recorded for the given type */
    fun hasWalk(walkType: WalkType): Boolean {
        return walkMap.containsKey(walkType)
    }

    /** Sums the sample sizes of every recorded walk among the allowed walk types */
    val sampleSize: Int
        get() {
            var total = 0
            for (walkType in allowedWalkTypes) {
                val walk = walkMap[walkType]
                if (walk != null) {
                    total += walk.sampleSize
                }
            }
            return total
    }

    /**
     * Restricts how many walk types are expected for this walk number
     * (must be 1–4; anything else falls back to all WalkType values).
     * Returns this holder for chaining.
     */
    fun setAllowedWalkTypes(numOfWalkTypesAllowed: Int): WalkHolder {
        allowedWalkTypes = if (numOfWalkTypesAllowed in 1..4) {
            WalkType.values().copyOfRange(0, numOfWalkTypesAllowed)
        } else {
            WalkType.values()
        }
        return this
    }
}