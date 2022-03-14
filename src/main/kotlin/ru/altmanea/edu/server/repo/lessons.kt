package ru.altmanea.edu.server.repo

import ru.altmanea.edu.server.model.Lesson

val lessonsRepo = ListRepo<Lesson>()

val lessonsRepoTestData = listOf(
    Lesson("Math"),
    Lesson("Phys"),
    Lesson("Story"),
)