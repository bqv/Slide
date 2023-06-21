package ltd.ucode.lemmy.data

import ltd.ucode.lemmy.data.type.PersonView
import ltd.ucode.slide.data.IUser

class LemmyUser(val instance: String, val data: PersonView) : IUser() {
    override val name: String
        get() = "${data.person.name}"
}
