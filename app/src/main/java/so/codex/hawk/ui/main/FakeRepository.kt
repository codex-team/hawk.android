package so.codex.hawk.ui.main

import so.codex.hawk.ui.data.UiProject

/**
 * Debug object.
 */
object FakeRepository {
    private const val fakeImageUrlOne = "https://codex.so/public/app/img/external/codex2x.png"
    private const val fakeImageUrlTwo =
        "https://cdn.pixabay.com/photo/2015/07/30/11/04/bike-867229_960_720.jpg"
    private const val fakeImageUrlThree =
        "https://cdn.pixabay.com/photo/2018/01/25/16/18/pinterest-3d-3106488_960_720.jpg"

    fun getProjects(): List<Project> {
        return listOf(
            Project(
                "a",
                "Hawk",
                "TypeError: Cannot read property 'indexOf'. Use getProperty().",
                fakeImageUrlOne,
                7.toBadge()
            ),
            Project(
                "b",
                "Project B",
                "TypeError: Cannot read property 'indexOf'. Use getProperty().",
                "",
                77.toBadge()
            ),
            Project(
                "c",
                "Project C",
                "TypeError: Cannot read property 'indexOf'. Use getProperty().",
                fakeImageUrlTwo,
                777.toBadge()
            ),
            Project(
                "d",
                "Project D",
                "TypeError: Cannot read property 'indexOf'. Use getProperty().",
                fakeImageUrlThree,
                7777.toBadge()
            ),
            Project(
                "f",
                "Project F",
                "TypeError: Cannot read property 'indexOf'. Use getProperty().",
                "",
                77777.toBadge()
            ),
            Project(
                "1",
                "Project 1",
                "TypeError: Cannot read property 'indexOf'. Use getProperty().",
                fakeImageUrlOne,
                1234567.toBadge()
            ),
            Project(
                "5",
                "Project 5",
                "TypeError: Cannot read property 'indexOf'. Use getProperty().",
                "",
                1234567.toBadge()
            ),
            Project(
                "8",
                "Project 8",
                "TypeError: Cannot read property 'indexOf'. Use getProperty().",
                "",
                11.toBadge()
            )
        )
    }
}