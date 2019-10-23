package de.rtrx.a

import de.rtrx.a.database.DDL
import kotlinx.coroutines.*
import mu.KotlinLogging
import net.dean.jraw.RedditClient
import net.dean.jraw.http.OkHttpNetworkAdapter
import net.dean.jraw.http.UserAgent
import net.dean.jraw.oauth.Credentials
import net.dean.jraw.oauth.OAuthHelper
import kotlin.concurrent.thread

@ExperimentalCoroutinesApi
fun main(args: Array<String>) {
    Runtime.getRuntime().addShutdownHook(thread(false) {
        runBlocking { stop() }
    })
    val options = parseOptions(args)
    initConfig(options.get("configPath") as String?)

    DDL.init(
            createDDL = (options.get("createDDL") as Boolean?) ?: true,
            createFunctions = (options.get("createDBFunctions") as Boolean?) ?: false
    )

    runBlocking {

        val messageMonitor = MessageMonitor()
        val submissionMonitor = SubmissionMonitor(messageMonitor.filteredInbox)

        wait()
    }

}


val jobs = mutableListOf<Job>()
val reddit: RedditClient by lazy {

    val oauthCreds = Credentials.script(
        config[RedditSpec.credentials.username],
        config[RedditSpec.credentials.password],
        config[RedditSpec.credentials.clientID],
        config[RedditSpec.credentials.clientSecret]
    )

    val userAgent = UserAgent("linux", config[RedditSpec.credentials.appID], "0.9", config[RedditSpec.credentials.operatorUsername])

    val reddit = OAuthHelper.automatic(OkHttpNetworkAdapter(userAgent), oauthCreds)
    reddit.logHttp = false
    return@lazy reddit
}

val logger by lazy {
    System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, config[LoggingSpec.logLevel])
    KotlinLogging.logger { }
}

suspend fun wait(){
    joinAll(*jobs.toTypedArray())
}

suspend fun stop(){
    jobs.forEach { it.cancelAndJoin() }
}
