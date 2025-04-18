package tachiyomi.domain.library.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class LibraryFlagsTest {

    @Test
    fun `Check the amount of flags`() {
        LibraryDisplayMode.values.size shouldBe 4
        LibrarySort.types.size shouldBe 11
        LibrarySort.directions.size shouldBe 2
    }

    @Test
    fun `Test Flag plus operator (LibrarySort)`() {
        val animecurrent = LibrarySort(
            LibrarySort.Type.LastSeen,
            LibrarySort.Direction.Ascending,
        )
        val newanime = LibrarySort(
            LibrarySort.Type.DateAdded,
            LibrarySort.Direction.Ascending,
        )
        val animeflag = animecurrent + newanime

        animeflag shouldBe 0b01011100
    }

    @Test
    fun `Test Flag plus operator`() {
        val animesort = LibrarySort(
            LibrarySort.Type.DateAdded,
            LibrarySort.Direction.Ascending,
        )

        animesort.flag shouldBe 0b01011100
    }

    @Test
    fun `Test Flag plus operator with old flag as base`() {
        val currentanimeSort = LibrarySort(
            LibrarySort.Type.UnseenCount,
            LibrarySort.Direction.Descending,
        )
        currentanimeSort.flag shouldBe 0b00001100

        val animesort = LibrarySort(
            LibrarySort.Type.DateAdded,
            LibrarySort.Direction.Ascending,
        )
        val animeflag = animesort.flag + animesort

        animeflag shouldBe 0b01011100
        animeflag shouldNotBe currentanimeSort.flag
    }

    @Test
    fun `Test default flags`() {
        val animesort = LibrarySort.default
        val animeflag = animesort.type + animesort.direction

        animeflag shouldBe 0b01000000
    }
}
