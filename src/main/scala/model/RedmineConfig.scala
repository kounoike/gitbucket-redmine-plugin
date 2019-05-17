package model

trait RedmineConfigComponent { self: gitbucket.core.model.Profile =>
  import profile.api._
  import self._

  lazy val RedmineConfigs = TableQuery[RedmineConfigs]

  class RedmineConfigs(tag: Tag) extends Table[RedmineConfig](tag, "REDMINE_CONFIG") {
    val userName = column[String]("USER_NAME", O PrimaryKey)
    val repositoryName = column[String]("REPOSITORY_NAME")
    val redmineUrl = column[String]("REDMINE_URL")
    val redmineProject = column[String]("REDMINE_PROJECT")
    val apiKey = column[String]("REDMINE_API_KEY")

    def * = (userName, repositoryName, redmineUrl, redmineProject, apiKey) <> (RedmineConfig.tupled, RedmineConfig.unapply)
  }
}

case class RedmineConfig(
  userName: String,
  repositoryName: String,
  redmineUrl: String,
  redmineProject: String,
  apiKey: String
)
