/*
 * The MIT License
 *
 * Copyright (c) 2016 Fulcrum Genomics
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */
package com.fulcrumgenomics.umi

import com.fulcrumgenomics.bam.api.SamSource
import com.fulcrumgenomics.cmdline.FgBioMain.FailureException
import com.fulcrumgenomics.commons.io.PathUtil
import com.fulcrumgenomics.testing.UnitSpec
import com.fulcrumgenomics.util.{Io, ReadStructure}

/**
  * Tests for AnnotateBamWithUmis
  */

class AnnotateBamWithUmisTest extends UnitSpec {
  private val dir    = PathUtil.pathTo("src/test/resources/com/fulcrumgenomics/umi")
  private val sam    = dir.resolve("annotate_umis.sam")
  private val fq     = dir.resolve("annotate_umis.fastq")
  private val umiBasesTag = ConsensusTags.UmiBases
  private val umiQualsTag = ConsensusTags.UmiQuals

  "AnnotateBamWithUmis" should "successfully add UMIs to a BAM in" in {
    val out       = makeTempFile("with_umis.", ".bam")
    val annotator = new AnnotateBamWithUmis(input=sam, fastq=fq, output=out, attribute=umiBasesTag)
    annotator.execute()
    SamSource(out).foreach(rec => {
      rec[String](umiBasesTag) shouldBe rec.basesString.substring(0,8)
    })
  }
  
  it should "successfully add UMIs (bases and qualities) to a BAM in" in {
    val out       = makeTempFile("with_umis.", ".bam")
    val annotator = new AnnotateBamWithUmis(input=sam, fastq=fq, output=out, attribute=umiBasesTag, qualAttribute=Some(umiQualsTag))
    annotator.execute()
    SamSource(out).foreach(rec => {
      println(rec.basesString)
      println(rec.qualsString)
      rec[String](umiBasesTag) shouldBe rec.basesString.substring(0,8)
      rec[String](umiQualsTag) shouldBe rec.qualsString.substring(0,8)
    })
  }

  it should "successfully add UMIs to a BAM in when the fastq is sorted" in {
    val out       = makeTempFile("with_umis.", ".bam")
    val annotator = new AnnotateBamWithUmis(input=sam, fastq=fq, output=out, attribute=umiBasesTag, sorted=true)
    annotator.execute()
    SamSource(out).foreach(rec => {
      rec[String](umiBasesTag) shouldBe rec.basesString.substring(0,8)
    })
  }

  it should "fail if one or more reads doesn't have a UMI" in {
    val out     = makeTempFile("with_umis.", ".bam")
    val shortFq = makeTempFile("missing_umis.", ".fq.gz")
    Io.writeLines(shortFq, Io.readLines(fq).toSeq.dropRight(8))
    val annotator = new AnnotateBamWithUmis(input=sam, fastq=shortFq, output=out, attribute=umiBasesTag)
    an[FailureException] shouldBe thrownBy { annotator.execute() }
  }

  it should "fail if one or more reads doesn't have a UMI when the fastq is sorted" in {
    val out     = makeTempFile("with_umis.", ".bam")
    val shortFq = makeTempFile(s"missing_umis.", ".fq.gz")
    Io.writeLines(shortFq, Io.readLines(fq).toSeq.dropRight(8))
    val annotator = new AnnotateBamWithUmis(input=sam, fastq=shortFq, output=out, attribute=umiBasesTag, sorted=true)
    an[FailureException] shouldBe thrownBy { annotator.execute() }
  }

  it should "fail there are extra reads in the fastq not in the bam when the fastq is sorted" in {
    val out    = makeTempFile("with_umis.", ".bam")
    val longFq = makeTempFile(s"extra_umis.", ".fq.gz")
    Io.writeLines(longFq, Io.readLines(fq))
    Io.writeLines(longFq, Seq("@not_a_flowcell:1:1101:10060:3200/2 2:N:0:19","GATCTTGG","+","-,86,,;:"))
    val annotator = new AnnotateBamWithUmis(input=sam, fastq=longFq, output=out, attribute=umiBasesTag, sorted=true)
    an[FailureException] shouldBe thrownBy { annotator.execute() }
  }

  it should "successfully add UMIs to a BAM with a given read structure in" in {
    val out       = makeTempFile("with_umis.", ".bam")
    val annotator = new AnnotateBamWithUmis(input=sam, fastq=fq, output=out, attribute=umiBasesTag, readStructure=ReadStructure("2B4M+B"))
    annotator.execute()
    SamSource(out).foreach(rec => {
      rec[String](umiBasesTag) shouldBe rec.basesString.substring(2,6)
    })
  }
}
