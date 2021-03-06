package ssscs

import java.text.SimpleDateFormat
import org.rogach.scallop.ScallopConf
import ssscs.crawler._
import ssscs.outputter._

object Ssscs {

  def main(args: Array[String]) {
    object Conf extends ScallopConf(args) {
      version("ssscs 0.1v")

      val count = opt[Int]("count", descr = "How many podcasts to crawl", default = Some(100) )
      val until = opt[String]("until", descr = "Only crawl the podcasts older than this date. " +
                                               "Example: 2013/05/12")

      val onlyTranscript = opt[Boolean]("only-transcript",
        short = 't',
        descr = "Only crawl transcripts",
        default = Some(false))
      val onlyPodcast = opt[Boolean]("only-podcast",
        short = 'p',
        descr = "Only crawl podcasts(mp3)",
        default = Some(false))

      val outputDir = opt[String]("output-directory",
        short = 'd',
        descr = "Where to store crawled files",
        default = Some("output")
      )
      val format = opt[String]("format",
        descr = "Output format. Support 'txt', 'pdf' and 'single-pdf'",
        default = Some("txt"),
        validate = (format) => (List("text", "pdf", "single-pdf").contains(format)))

    }

    def parseCrawlingConfig(conf: Conf.type): CrawlingConfig = {
      val dateFormat = new SimpleDateFormat("yyyy/MM/dd")
      val until = conf.until.get.map(str => dateFormat.parse(str))

      CrawlingConfig(conf.count(), until,
                     conf.onlyTranscript(), conf.onlyPodcast())
    }

    def parseOutputConfig(conf: Conf.type): OutputConfig = {
      OutputConfig(conf.outputDir(), conf.format())
    }

    val crawlingConfig = parseCrawlingConfig(Conf)
    val infos = new InfoCrawler(crawlingConfig).crawlInfos()
    val articles = new ContentCrawler(crawlingConfig).crawlArticles(infos)

    val outputConfig = parseOutputConfig(Conf)

    new Mp3Outputter().output(articles, outputConfig.dirPath)
    outputConfig.format match {
      case "text" | "txt" => new TextOutputter().output(articles, outputConfig.dirPath)
      case "pdf" => new PdfOutputter().output(articles, outputConfig.dirPath)
      case "single-pdf" => new SinglePdfOutputter().output(articles, outputConfig.dirPath)
      case format => println(s"Unsupported format: ${format}")
    }
  }

}
