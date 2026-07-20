package edu.wpi.alcogaitdatagatherer.models

/**
 * Created by Adonay on 9/27/2017.
 */
enum class Gender(
    private val genderString: String
) {
    MALE("Male"),
    FEMALE("Female");

    override fun toString(): String {
        return genderString
    }
}