package ltd.ucode.lemmy.data

import ltd.ucode.lemmy.data.type.Person
import ltd.ucode.lemmy.data.type.PersonAggregates
import ltd.ucode.slide.data.IUser

class LemmyUser(val instance: String, val person: Person,
                private val counts: PersonAggregates? = null) : IUser() {
    override val name: String
        get() = "${person.name}"
}
