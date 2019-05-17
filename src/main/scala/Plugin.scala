import controller.RedmineController
import gitbucket.core.controller.Context
import gitbucket.core.plugin.Link
import gitbucket.core.service.RepositoryService.RepositoryInfo
import io.github.gitbucket.solidbase.migration.LiquibaseMigration
import io.github.gitbucket.solidbase.model.Version

class Plugin extends gitbucket.core.plugin.Plugin {
  override val pluginId: String = "redmine"
  override val pluginName: String = "Redmine Integration Plugin"
  override val description: String = "Redmine and GitBucket integration plug-in"
  override val versions: List[Version] = List(
    new Version("0.0.1", new LiquibaseMigration("update/gitbucket-redmine_0.0.1.xml"))
  )

  override val repositorySettingTabs = Seq(
    (repository: RepositoryInfo, context: Context) => Some(Link("redmine", "Redmine", "settings/redmine"))
  )

  override val controllers = Seq(
    "/*" -> new RedmineController()
  )
}
