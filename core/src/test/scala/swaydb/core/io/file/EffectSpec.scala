/*
 * Copyright (c) 2020 Simer JS Plaha (simer.j@gmail.com - @simerplaha)
 *
 * This file is a part of SwayDB.
 *
 * SwayDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * SwayDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with SwayDB. If not, see <https://www.gnu.org/licenses/>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7:
 * If you modify this Program or any covered work, only by linking or
 * combining it with separate works, the licensors of this Program grant
 * you additional permission to convey the resulting work.
 */

package swaydb.core.io.file

import java.nio.file.Paths

import swaydb.IO
import swaydb.IOValues._
import swaydb.core.TestBase
import swaydb.core.TestData._
import swaydb.core.util.{Benchmark, Extension}
import swaydb.data.util.StorageUnits._

class EffectSpec extends TestBase {

  "fileId" should {

    "value the file id" in {
      Effect.fileId(Paths.get("/one/1.log")).runRandomIO.right.value shouldBe(1, Extension.Log)
      Effect.fileId(Paths.get("/one/two/10.log")).runRandomIO.right.value shouldBe(10, Extension.Log)
      Effect.fileId(Paths.get("/one/two/three/1000.seg")).runRandomIO.right.value shouldBe(1000, Extension.Seg)
    }

    "fail if the file's name is not an integer" in {
      val path = Paths.get("/one/notInt.log")
      IO(Effect.fileId(path)).left.value shouldBe swaydb.Exception.NotAnIntFile(path)
    }

    "fail if the file has invalid extension" in {
      val path = Paths.get("/one/1.txt")
      IO(Effect.fileId(path)).left.runRandomIO.right.value shouldBe swaydb.Exception.UnknownExtension(path)
    }
  }

  "folderId" should {
    "value the folderId" in {
      Effect.folderId(Paths.get("/one/1")) shouldBe 1
      Effect.folderId(Paths.get("/one/two/10")) shouldBe 10
      Effect.folderId(Paths.get("/one/two/three/1000")) shouldBe 1000
    }
  }

  "incrementFileId" should {
    "return a new file path with incremented file id" in {
      Effect.incrementFileId(Paths.get("/one/1.log")).runRandomIO.right.value shouldBe Paths.get("/one/2.log")
      Effect.incrementFileId(Paths.get("/one/two/10.log")).runRandomIO.right.value shouldBe Paths.get("/one/two/11.log")
      Effect.incrementFileId(Paths.get("/one/two/three/1000.seg")).runRandomIO.right.value shouldBe Paths.get("/one/two/three/1001.seg")
    }
  }

  "incrementFolderId" should {
    "return a new file path with incremented folder id" in {
      Effect.incrementFolderId(Paths.get("/one/1")) shouldBe Paths.get("/one/2")
      Effect.incrementFolderId(Paths.get("/one/two/10")) shouldBe Paths.get("/one/two/11")
      Effect.incrementFolderId(Paths.get("/one/two/three/1000")) shouldBe Paths.get("/one/two/three/1001")
    }
  }

  "files" should {
    "fetch all the files in sorted order" in {
      val dir = createRandomIntDirectory
      val actual =
        Seq(
          dir.resolve(s"1.${Extension.Log}"),
          dir.resolve(s"4.${Extension.Log}"),
          dir.resolve(s"99.${Extension.Log}"),
          dir.resolve(s"2.${Extension.Log}"),
          dir.resolve(s"299.${Extension.Log}"),
          dir.resolve(s"3.${Extension.Log}"),
          dir.resolve(s"10.${Extension.Log}"),
          dir.resolve(s"33.${Extension.Log}")
        )
      actual.foreach {
        path =>
          Effect.createFile(path).runRandomIO.right.value
      }

      val expect =
        Seq(
          dir.resolve(s"1.${Extension.Log}"),
          dir.resolve(s"2.${Extension.Log}"),
          dir.resolve(s"3.${Extension.Log}"),
          dir.resolve(s"4.${Extension.Log}"),
          dir.resolve(s"10.${Extension.Log}"),
          dir.resolve(s"33.${Extension.Log}"),
          dir.resolve(s"99.${Extension.Log}"),
          dir.resolve(s"299.${Extension.Log}")
        )

      Effect.files(dir, Extension.Log) shouldBe expect
    }
  }

  "folders" should {
    "fetch all the folders in sorted order" in {
      val dir = createRandomIntDirectory
      val actual =
        Seq(
          dir.resolve("1"),
          dir.resolve("10"),
          dir.resolve("7"),
          dir.resolve("15"),
          dir.resolve("7676"),
          dir.resolve("123"),
          dir.resolve("0"),
          dir.resolve("5454")
        )
      actual.foreach {
        path =>
          Effect.createDirectoryIfAbsent(path)
      }

      val expect =
        Seq(
          dir.resolve("0"),
          dir.resolve("1"),
          dir.resolve("7"),
          dir.resolve("10"),
          dir.resolve("15"),
          dir.resolve("123"),
          dir.resolve("5454"),
          dir.resolve("7676")
        )

      Effect.folders(dir) shouldBe expect
    }
  }

  "segmentFilesOnDisk" should {
    "fetch all segment files in order" in {
      val dir1 = createRandomIntDirectory
      val dir2 = createRandomIntDirectory
      val dir3 = createRandomIntDirectory
      val dirs = Seq(dir1, dir2, dir3)

      dirs foreach {
        dir =>
          val actual =
            Seq(
              dir.resolve("1.seg"),
              dir.resolve("10.seg"),
              dir.resolve("7.seg"),
              dir.resolve("15.seg"),
              dir.resolve("7676.seg"),
              dir.resolve("123.seg"),
              dir.resolve("0.seg"),
              dir.resolve("5454.seg")
            )
          actual.foreach {
            path =>
              Effect.createFileIfAbsent(path)
          }
      }

      val expect =
        Seq(
          dir1.resolve("0.seg"),
          dir2.resolve("0.seg"),
          dir3.resolve("0.seg"),
          dir1.resolve("1.seg"),
          dir2.resolve("1.seg"),
          dir3.resolve("1.seg"),
          dir1.resolve("7.seg"),
          dir2.resolve("7.seg"),
          dir3.resolve("7.seg"),
          dir1.resolve("10.seg"),
          dir2.resolve("10.seg"),
          dir3.resolve("10.seg"),
          dir1.resolve("15.seg"),
          dir2.resolve("15.seg"),
          dir3.resolve("15.seg"),
          dir1.resolve("123.seg"),
          dir2.resolve("123.seg"),
          dir3.resolve("123.seg"),
          dir1.resolve("5454.seg"),
          dir2.resolve("5454.seg"),
          dir3.resolve("5454.seg"),
          dir1.resolve("7676.seg"),
          dir2.resolve("7676.seg"),
          dir3.resolve("7676.seg")
        )

      Effect.segmentFilesOnDisk(dirs) shouldBe expect
    }
  }

  "benchmark" in {
    val fileSize = 4.mb
    val flattenBytes = randomBytesSlice(fileSize)
    val groupBytes = flattenBytes.groupedSlice(8)

    //20.mb
    //0.067924621 seconds
    //4.mb
    //0.057647201 seconds & 0.047565694 seconds
    val groupedPath = Benchmark("groupBytes")(Effect.write(randomFilePath, groupBytes))
    Effect.readAll(groupedPath) shouldBe flattenBytes

    //20.mb
    //0.077162871 seconds
    //4.mb
    //0.05330862 seconds & 0.045989919 seconds
    val flattenedPath = Benchmark("flattenBytes")(Effect.write(randomFilePath, flattenBytes))
    Effect.readAll(flattenedPath) shouldBe flattenBytes
  }
}
