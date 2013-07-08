package controllers

import edu.nyu.cs.javagit.api._
import scala.collection.JavaConversions._
import scala.sys.process._

object Utility {
  lazy val hostname = java.net.InetAddress.getLocalHost.getHostName()
  
  lazy val rootDir = """/home/%s/testServerApi""" format(System.getProperty("user.name"))
  lazy val wtDir = """%s/Expert""" format(rootDir)
  lazy val makeDirFile = new java.io.File("""%s/Expert/src/code/""" format(rootDir))

  println("rootDir: %s, wtDir: %s, makeDirFile: %s".format(rootDir, wtDir, makeDirFile))

  private def wt     = WorkingTree.getInstance(wtDir)
  private def dotGit = wt.getDotGit()
  
  def executeGitCommand(cmd : String) = {
    println("Will execute git command: [%s]".format(cmd))
    "git --git-dir=%s/.git %s".format(wtDir, cmd).!!
  }
  
  def commits =
    dotGit.getLog().toList.take(20).map(x => (x.getSha(), x.getDateString(), x.getAuthor(), x.getMessage()))

  def branches =
    //dotGit.getBranches().toList.map(_.getName())
    executeGitCommand("branch -a").split("\n").map(x => x.substring(1).trim().split("""/""").toList.last).toSet.toList

  def currentBranch =
    try { wt.getCurrentBranch().getName() }
    catch { case _:Exception => ""}
    
  def gitCheckoutAll = 
    //executeGitCommand("checkout .")
    executeGitCommand("reset HEAD --hard")

  def checkoutBranch(branchName : String) {
    val command = """checkout -f %s""" format(branchName)
    val fallbackCommand = """checkout -b %s origin/%s""" format(branchName, branchName)
    executeGitCommand("checkout .")
    try { executeGitCommand(command) } 
    catch { case _:Exception => executeGitCommand(fallbackCommand) }
  }
  
  def currentCommit = 
    executeGitCommand("""log --pretty=oneline""").split("\n").map(_.split(" ").toList).map({case x::xs => (x, xs.mkString(" ")) }).head
}