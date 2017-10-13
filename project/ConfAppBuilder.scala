import play.PlayRunHook
import sbt._
import java.net.InetSocketAddress
import java.io.File

case class ConfAppBuilder(base: File) {
  def run() = {
    val sourceFolder = new File(base, "schedule")
    val buildFolder = new File(sourceFolder, "dist")
    val publicFolder = new File(base, "public/schedule")
    val ignoredFiles = List("index.html")
    
    if(System.getProperty("os.name") startsWith "Windows") {
      Process("npm.cmd run buildProd", sourceFolder) !
    } else {
      Process("npm run buildProd", sourceFolder) !
    }

    buildFolder.list()
      .filter(f => !ignoredFiles.contains(f))
      .map(fileName => {
        val sourceFile = new File(buildFolder, fileName)
        val destinationFile = new File(publicFolder, fileName)
        if(sourceFile.isFile()) {
          Process(sourceFile) #> destinationFile ! 
        } else {
          IO.copyDirectory(sourceFile, destinationFile, overwrite = true) 
        }
      })
  }
}

