package tech.eritquearcus.miraicp

import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import net.mamoe.mirai.Bot
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.Mirai
import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.NormalMember
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.MessageSerializers
import net.mamoe.mirai.message.code.MiraiCode
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import net.mamoe.mirai.utils.*
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import net.mamoe.mirai.utils.MiraiLogger.Companion.setDefaultLoggerCreator
import net.mamoe.mirai.utils.RemoteFile.Companion.uploadFile
import org.fusesource.jansi.AnsiConsole
import org.json.JSONObject
import tech.eritquearcus.miraicp.KotlinMain.now_tag
import java.io.File
import java.lang.Long.valueOf
import java.net.URL
import java.util.*
import kotlin.concurrent.schedule

object KotlinMain {
    private val json = Json{
        Mirai
        serializersModule = MessageSerializers.serializersModule
    }
    const val now_tag = "v2.6.3-RC"
    private var friend_cache = ArrayList<NormalMember>(0)
    lateinit var dll_name:String
    private lateinit var cpp: CPP_lib
    private lateinit var logger:MiraiLogger
    private val gson = Gson()

    //日志部分实现
    fun BasicSendLog(log: String) {
        logger.info(log)
    }

    fun SendWarning(log: String) {
        logger.warning(log)
    }

    fun SendError(log: String) {
        logger.error(log)
    }

    //发送消息部分实现 MiraiCode

    suspend fun Send0(message: Message, c:Config.Contact):String{
        val AIbot = Bot.getInstance(c.botid)
        when(c.type){
            1->{
                logger.info("Send message for(${c.id}) is $message")
                val f = AIbot.getFriend(c.id) ?: let {
                    logger.error("发送消息找不到好友，位置:K-Send()，id:${c.id}")
                    return "E1"
                }
                return json.encodeToString(MessageSource.Serializer,
                    f.sendMessage(message).source)
            }
            2->{
                logger.info("Send message for Group(${c.id}) is $message")
                val g = AIbot.getGroup(c.id) ?: let {
                    logger.error("发送群消息异常找不到群组，位置K-SendG，gid:${c.id}")
                    return "E1"
                }
                return json.encodeToString(MessageSource.Serializer,
                    g.sendMessage(message).source)
            }
            3->{
                logger.info("Send message for a member(${c.id}) is $message")
                for (a in friend_cache) {
                    if (a.id == c.id && a.group.id == c.groupid) {
                        return json.encodeToString(MessageSource.Serializer,
                            a.sendMessage(message).source)
                    }
                }
                val G = AIbot.getGroup(c.groupid) ?: let {
                    logger.error("发送消息找不到群聊，位置K-Send()，id:${c.groupid}")
                    return "E1"
                }
                val f = G[c.id] ?: let {
                    logger.error("发送消息找不到群成员，位置K-Send()，id:${c.id}，gid:${c.groupid}")
                    return "E2"
                }
                return json.encodeToString(MessageSource.Serializer, f.sendMessage(message).source)
            }
            else->return "E2"
        }
    }

    suspend fun SendMsg(message: String, c:Config.Contact):String{
        val m = MessageChainBuilder()
        m.add(message)
        return Send0(m.asMessageChain(), c)
    }

    suspend fun SendMiraiCode(message: String, c:Config.Contact):String{
        return Send0(MiraiCode.deserializeMiraiCode(message), c)
    }

    fun RefreshInfo(c: Config.Contact): String{
        val AIbot = Bot.getInstance(c.botid)
        when(c.type){
            1->{
                val f = AIbot.getFriend(c.id) ?: let {
                    logger.error("找不到对应好友，位置:K-GetNickOrNameCard()，id:${c.id}")
                    return "E1"
                }
                return gson.toJson(Config.ContactInfo(f.nick, f.avatarUrl))
            }
            2->{
                val g = AIbot.getGroup(c.id)?:let{
                    logger.error("取群名称找不到群,位置K-GetNickOrNameCard(), gid:${c.id}")
                    return "E1"
                }
                return gson.toJson(Config.ContactInfo(g.name, g.avatarUrl))
            }
            3->{
                for (a in friend_cache) {
                    if (a.id == c.id && a.group.id == c.groupid) {
                        return a.nameCardOrNick
                    }
                }

                val group = AIbot.getGroup(c.groupid) ?: let {
                    logger.error("取群名片找不到对应群组，位置K-GetNickOrNameCard()，gid:${c.groupid}")
                    return "E1"
                }
                val m = group[c.id] ?: let {
                    logger.error("取群名片找不到对应群成员，位置K-GetNickOrNameCard()，id:${c.id}, gid:${c.groupid}")
                    return "E2"
                }
                return gson.toJson(Config.ContactInfo(m.nameCardOrNick, m.avatarUrl))
            }
            4->{
                return gson.toJson(Config.ContactInfo(AIbot.nick, AIbot.avatarUrl))
            }
            else->{
                return "EE"
            }
        }
    }

    //取群成员列表
    fun QueryML(c: Config.Contact): String {
        val AIbot = Bot.getInstance(c.botid)
        val g = AIbot.getGroup(c.id) ?: let {
            logger.error("取群成员找不到群,位置K-QueryML")
            return "E1"
        }
        val m = java.util.ArrayList<Long>()
        g.members.forEach{
            m.add(it.id)
        }
        return gson.toJson(m)
    }

    fun QueryBFL(bid: Long): String{
        val AIbot = Bot.getInstance(bid)
        val tmp = java.util.ArrayList<Long>()
        AIbot.friends.forEach {
            tmp.add(it.id)
        }
        return gson.toJson(tmp)
    }
    fun QueryBGL(bid: Long): String{
        val AIbot = Bot.getInstance(bid)
        val tmp = java.util.ArrayList<Long>()
        AIbot.groups.forEach {
            tmp.add(it.id)
        }
        return gson.toJson(tmp)
    }

    //图片部分实现

    suspend fun uploadImg(file: String, c: Config.Contact):String{
        val AIbot = Bot.getInstance(c.botid)
        when(c.type){
            1->{
                val temp = AIbot.getFriend(c.id) ?: let {
                    logger.error("发送图片找不到对应好友,位置:K-uploadImgFriend(),id:${c.id}")
                    return "E1"
                }
                return try {
                    File(file).uploadAsImage(temp).imageId
                } catch (e: OverFileSizeMaxException) {
                    logger.error("图片文件过大超过30MB,位置:K-uploadImgGroup(),文件名:$file")
                    "E2"
                } catch (e: NullPointerException) {
                    logger.error("上传图片文件名异常,位置:K-uploadImgGroup(),文件名:$file")
                    "E3"
                }
            }
            2->{
                val temp = AIbot.getGroup(c.id) ?: let {
                    logger.error("发送图片找不到对应群组,位置:K-uploadImgGroup(),id:${c.id}")
                    return "E1"
                }
                return try {
                    File(file).uploadAsImage(temp).imageId
                } catch (e: OverFileSizeMaxException) {
                    logger.error("图片文件过大超过30MB,位置:K-uploadImgGroup(),文件名:$file")
                    "E2"
                } catch (e: NullPointerException) {
                    logger.error("上传图片文件名异常,位置:K-uploadImgGroup(),文件名:$file")
                    "E3"
                }
            }
            3->{
                val temp = AIbot.getGroup(c.groupid) ?: let {
                    logger.error("发送图片找不到对应群组,位置:K-uploadImgGroup(),id:${c.groupid}")
                    return "E1"
                }
                val temp1 = temp[c.id] ?: let {
                    logger.error("发送图片找不到目标成员,位置:K-uploadImgMember(),成员id:${c.id},群聊id:${c.groupid}")
                    return "E2"
                }
                return try {
                    File(file).uploadAsImage(temp1).imageId
                } catch (e: OverFileSizeMaxException) {
                    logger.error("图片文件过大超过30MB,位置:K-uploadImgGroup(),文件名:$file")
                    "E3"
                } catch (e: NullPointerException) {
                    logger.error("上传图片文件名异常,位置:K-uploadImgGroup(),文件名:$file")
                    "E4"
                }
            }
            else->{
                return ""
            }
        }
    }

    suspend fun QueryImg(id: String): String {
        return Image(id).queryUrl()
    }

    //recall
    suspend fun recallMsg(a:String): String {
        val source = json.decodeFromString(MessageSource.Serializer,a)
        try{
            source.recall()
        }catch (e:PermissionDeniedException){
            logger.error("机器人无权限撤回")
            return "E1"
        }catch(e:IllegalStateException){
            logger.error("该消息已被撤回")
            return "E2"
        }
        return "Y"
    }

    //定时任务
    fun scheduling(time: Long, id: String) {
        Timer("SettingUp", false).schedule(time) {
            cpp.Event(
                Gson().toJson(
                    Config.TimeOutEvent(
                        id
                    )
                )
            )
        }
    }

    //禁言
    suspend fun mute(time:Int, c: Config.Contact):String{
        val AIbot = Bot.getInstance(c.botid)
        val group = AIbot.getGroup(c.groupid) ?: let{
            logger.error("禁言找不到对应群组，位置K-mute()，gid:${c.groupid}")
            return "E1"
        }
        val member = group[c.id] ?: let {
            logger.error("禁言找不到对应群成员，位置K-mute()，id:${c.id}, gid:${c.id}")
            return "E2"
        }
        try {
            member.mute(time)
        }catch (e: PermissionDeniedException){
            logger.error("执行禁言失败机器人无权限，位置:K-mute()，目标群id:${c.groupid}，目标成员id:${c.id}")
            return "E3"
        }catch (e:IllegalStateException){
            logger.error("执行禁言失败禁言时间超出0s~30d，位置:K-mute()，时间:$time")
            return "E4"
        }
        return "Y"
    }

    private suspend fun fileInfo0(temp: RemoteFile):String{
        val dinfo = temp.getDownloadInfo()!!
        val finfo = temp.getInfo()!!
        return gson.toJson(Config.FileInfo(
            id = finfo.id,
            name = finfo.name,
            path = finfo.path,
            dinfo = Config.DInfo(dinfo.url, dinfo.md5.toString(), dinfo.sha1.toString()),
            finfo = Config.FInfo(
                finfo.length,
                finfo.uploaderId,
                finfo.downloadTimes,
                finfo.uploaderId,
                finfo.lastModifyTime
            )
        )
        )
    }
    suspend fun sendFile(path: String, file: String, c: Config.Contact): String {
        val AIbot = Bot.getInstance(c.botid)
        val group = AIbot.getGroup(c.id) ?: let {
            logger.error("找不到对应群组，位置K-uploadfile()，gid:${c.id}")
            return "E1"
        }
        val f = File(file)
        if (!f.exists() || !f.isFile) {
            return "E2"
        }
        val tmp =
            try {
                group.uploadFile(path, f)
            } catch (e: IllegalStateException) {
                return "E3"
            } catch (e: Exception){
                logger.error(e.message)
                e.printStackTrace()
                return "E3"
            }
        tmp.sendTo(group)
        val temp = group.filesRoot.resolveById(tmp.id)?:let{
            logger.error("cannot find the file, 位置:K-uploadFile, id:${tmp.id}")
            return "E3"
        }
        val t = fileInfo0(temp)
        return t
    }

    private suspend fun remoteFileList(path: String, c: Config.Contact):String{
        val AIbot = Bot.getInstance(c.botid)
        val group = AIbot.getGroup(c.id) ?: let {
            logger.error("找不到对应群组，位置K-remoteFileInfo，gid:${c.id}")
            return "E1"
        }
        var tmp = "["
        group.filesRoot.resolve(path).listFilesCollection().forEach {
            tmp += "[\"${it.path}\", \"${it.id}\"],"
        }
        tmp = tmp.substring(0, tmp.length - 1)
        tmp += "]"
        return tmp
    }

    private suspend fun remoteFileInfo0(path: String, c:Config.Contact):String {
        val AIbot = Bot.getInstance(c.botid)
        val group = AIbot.getGroup(c.id) ?: let {
            logger.error("找不到对应群组，位置K-remoteFileInfo0，gid:${c.id}")
            return "E1"
        }
        val tmp = group.filesRoot.resolve(path)
        if (!tmp.isFile() || !tmp.exists()) {
            logger.error("cannot find the file,位置:K-remoteFileinfo0, path: $path")
            return "E2"
        }
        return fileInfo0(tmp)
    }

    suspend fun remoteFileInfo(path: String, id: String, c: Config.Contact):String{
        val AIbot = Bot.getInstance(c.botid)
        if(id == "")
            return remoteFileInfo0(path, c)
        if(id == "-1")
            return remoteFileList(path, c)
        val group = AIbot.getGroup(c.id) ?: let {
            logger.error("找不到对应群组，位置K-remoteFileInfo，gid:${c.id}")
            return "E1"
        }
        val tmp = group.filesRoot.resolve(path).resolveById(id)?:let{
            logger.error("cannot find the file,位置:K-remoteFileinfo, id:$id")
            return "E2"
        }
        return fileInfo0(tmp)
    }

    //查询权限
    fun kqueryM(c: Config.Contact): String{
        val AIbot = Bot.getInstance(c.botid)
        val group = AIbot.getGroup(c.groupid) ?: let {
            logger.error("查询权限找不到对应群组，位置K-queryM()，gid:${c.groupid}")
            return "E1"
        }
        val member = group[c.id] ?: let {
            logger.error("查询权限找不到对应群成员，位置K-queryM()，id:${c.id}, gid:${c.groupid}")
            return "E2"
        }
        return member.permission.level.toString()

    }

    suspend fun kkick(message: String, c: Config.Contact):String{
        val AIbot = Bot.getInstance(c.botid)
        val group = AIbot.getGroup(c.groupid) ?: let {
            logger.error("查询权限找不到对应群组，位置K-queryM()，gid:${c.groupid}")
            return "E1"
        }
        val member = group[c.id] ?: let {
            logger.error("查询权限找不到对应群成员，位置K-queryM()，id:${c.id}, gid:${c.id}")
            return "E2"
        }
        try {
            member.kick(message)
        }catch (e:PermissionDeniedException){
            return "E3"
        }
        return "Y"
    }

    //全员禁言
    fun muteall(sign: Boolean, c: Config.Contact):String{
        val AIbot = Bot.getInstance(c.botid)
        val g =AIbot.getGroup(c.id)?:let{
            logger.error("找不到群,位置:K-muteall, gid:${c.id}")
            return "E1"
        }
        try {
            g.settings.isMuteAll = sign
        }catch(e:PermissionDeniedException){
            return "E2"
        }
        return "Y"
    }

    //取群主
    fun getowner(c: Config.Contact):String{
        val AIbot = Bot.getInstance(c.botid)
        val g = AIbot.getGroup(c.id)?:let {
            logger.error("找不到群,位置:K-getowner,gid:${c.id}")
            return "E1"
        }
        return g.owner.id.toString()
    }

    //构建聊天记录
    suspend fun buildforwardMsg(text:String, bid: Long):String{
        val AIbot = Bot.getInstance(bid)
        val t = Gson().fromJson(text, Config.ForwardMessageJson::class.java)
        val c1:Contact = when(t.type) {
            1 -> AIbot.getFriend(t.id) ?: let {
                return "E1"
            }
            2 -> AIbot.getGroup(t.id) ?: let {
                return "E1"
            }
            3 -> (AIbot.getGroup(t.id) ?: let {
                return "E1"
            })[t.id2]?:let {
                return "E2"
            }
            else -> return "E3"
        }
        val c:Contact = when(t.content.type) {
            1 -> AIbot.getFriend(t.content.id) ?: let {
                return "E1"
            }
            2 -> AIbot.getGroup(t.content.id) ?: let {
                return "E1"
            }
            3 -> (AIbot.getGroup(t.content.id) ?: let {
                return "E1"
            })[t.content.id2]?:let {
                return "E2"
            }
            else -> return "E3"
        }
        val a = ForwardMessageBuilder(c)
        t.content.value.forEach {
            a.add(ForwardMessage.Node(it.id, it.time, it.name, MiraiCode.deserializeMiraiCode(it.message)))
        }
        a.build().sendTo(c1)
        return "Y"
    }

    @Suppress("INVISIBLE_MEMBER")
    suspend fun accpetFriendRequest(info: Config.NewFriendRequestSource): String {
        try {
            NewFriendRequestEvent(
                Bot.getInstance(info.botid),
                info.eventid,
                info.message,
                info.fromid,
                info.fromgroupid,
                info.fromnick
            ).accept()
        } catch (e: IllegalStateException) {
            return "E"
        }
        return "Y"
    }

    @Suppress("INVISIBLE_MEMBER")
    suspend fun rejectFriendRequest(info: Config.NewFriendRequestSource): String {
        try {
            NewFriendRequestEvent(
                Bot.getInstance(info.botid),
                info.eventid,
                info.message,
                info.fromid,
                info.fromgroupid,
                info.fromnick
            ).reject()
        } catch (e: IllegalStateException) {
            return "E"
        }
        return "Y"
    }

    @OptIn(MiraiInternalApi::class)
    @Suppress("INVISIBLE_MEMBER")
    suspend fun accpetGroupInvite(info: Config.GroupInviteSource): String {
        try {
            BotInvitedJoinGroupRequestEvent(
                Bot.getInstance(info.botid),
                info.eventid,
                info.inviterid,
                info.groupid,
                info.groupname,
                info.inviternick
            ).accept()
        } catch (e: IllegalStateException) {
            return "E"
        }
        return "Y"
    }

    @OptIn(MiraiInternalApi::class)
    @Suppress("INVISIBLE_MEMBER")
    suspend fun rejectGroupInvite(info: Config.GroupInviteSource): String {
        try {
            BotInvitedJoinGroupRequestEvent(
                Bot.getInstance(info.botid),
                info.eventid,
                info.inviterid,
                info.groupid,
                info.groupname,
                info.inviternick
            ).ignore()
        } catch (e: IllegalStateException) {
            return "E"
        }
        return "Y"
    }

    @MiraiExperimentalApi
    @MiraiInternalApi
    suspend fun main(id: Long, pass: String, path: String) {
        println("当前MiraiCP框架版本:$now_tag")
        setDefaultLoggerCreator { identity ->
            AnsiConsole.systemInstall()
            PlatformLogger(identity, AnsiConsole.out()::println, true)
        }
        dll_name = path
        println("启动成功!")
        println("github存储库:https://github.com/Nambers/MiraiCP")
        if (!File(dll_name).exists()) {
            println("文件$dll_name 不存在")
            return
        }else{
            dll_name = File(dll_name).absolutePath
        }
        val bot = BotFactory.newBot(id, pass) {
            fileBasedDeviceInfo()
        }.alsoLogin()
        logger=bot.logger
        cpp = CPP_lib()
        if(cpp.ver != now_tag){
            logger.error("警告:当前MiraiCP框架版本($now_tag)和加载的C++ SDK(${cpp.ver})不一致")
        }
        val globalEventChannel = bot.eventChannel
        logger.info(cpp.ver)//输出版本
        //配置文件目录 "${dataFolder.absolutePath}/"
        globalEventChannel.subscribeAlways<FriendMessageEvent> {
            //好友信息
            cpp.Event(
                gson.toJson(
                    Config.PrivateMessage(
                        Config.Contact(1, this.sender.id, 0, this.senderName, this.bot.id),
                        this.message.serializeToMiraiCode(),
                        json.encodeToString(
                            MessageSource.Serializer,
                            this.message[MessageSource]!!
                        )
                    )
                )
            )
        }
        globalEventChannel.subscribeAlways<GroupMessageEvent> {
            //群消息
            try {
                cpp.Event(
                    gson.toJson(
                        Config.GroupMessage(
                            Config.Contact(2, this.group.id, 0, this.group.name, this.bot.id),
                            Config.Contact(3, this.sender.id, this.group.id, this.senderName, this.bot.id),
                            this.message.serializeToMiraiCode(),
                            json.encodeToString(MessageSource.Serializer, this.message[MessageSource]!!)
                        )
                    )
                )
            } catch (e: Exception) {
                logger.error(e.message)
                e.printStackTrace()
            }
        }
        globalEventChannel.subscribeAlways<MemberLeaveEvent.Kick> {
            friend_cache.add(this.member)
            cpp.Event(
                gson.toJson(
                    Config.MemberLeave(
                        Config.Contact(2, this.group.id, 0, this.group.name, this.bot.id),
                        this.member.id,
                        1,
                        if (this.operator?.id == null) this.bot.id else this.operator!!.id
                    )
                )
            )
            friend_cache.remove(this.member)
        }
        globalEventChannel.subscribeAlways<MemberLeaveEvent.Quit> {
            friend_cache.add(this.member)
            cpp.Event(
                gson.toJson(
                    Config.MemberLeave(
                        Config.Contact(2, this.group.id, 0, this.group.name, this.bot.id),
                        this.member.id,
                        2,
                        this.member.id
                    )
                )
            )
            friend_cache.remove(this.member)
        }
        globalEventChannel.subscribeAlways<MemberJoinEvent.Retrieve> {
            cpp.Event(
                gson.toJson(
                    Config.MemberJoin(
                        Config.Contact(2, this.group.id, 0, this.group.name, this.bot.id),
                        Config.Contact(3, this.member.id, this.groupId, this.member.nameCardOrNick, this.bot.id),
                        3,
                        this.member.id
                    )
                )
            )
        }
        globalEventChannel.subscribeAlways<MemberJoinEvent.Active> {
            cpp.Event(
                gson.toJson(
                    Config.MemberJoin(
                        Config.Contact(2, this.group.id, 0, this.group.name, this.bot.id),
                        Config.Contact(3, this.member.id, this.groupId, this.member.nameCardOrNick, this.bot.id),
                        2,
                        this.member.id
                    )
                )
            )
        }
        globalEventChannel.subscribeAlways<MemberJoinEvent.Invite> {
            cpp.Event(
                gson.toJson(
                    Config.MemberJoin(
                        Config.Contact(2, this.group.id, 0, this.group.name, this.bot.id),
                        Config.Contact(3, this.member.id, this.groupId, this.member.nameCardOrNick, this.bot.id),
                        1,
                        this.invitor.id
                    )
                )
            )
        }
        globalEventChannel.subscribeAlways<NewFriendRequestEvent> {
            //自动同意好友申请
            cpp.Event(
                gson.toJson(
                    Config.NewFriendRequest(
                        Config.NewFriendRequestSource(
                            this.bot.id,
                            this.eventId,
                            this.message,
                            this.fromId,
                            this.fromGroupId,
                            this.fromNick
                        )
                    )
                )
            )

        }
        globalEventChannel.subscribeAlways<MessageRecallEvent.FriendRecall> {
            cpp.Event(
                gson.toJson(
                    Config.RecallEvent(
                        1,
                        this.authorId,
                        this.operatorId,
                        this.messageIds.map { it.toString() }.toTypedArray().contentToString(),
                        this.messageInternalIds.map { it.toString() }.toTypedArray().contentToString(),
                        this.messageTime,
                        0,
                        this.bot.id
                    )
                )
            )

        }
        globalEventChannel.subscribeAlways<MessageRecallEvent.GroupRecall> {
            cpp.Event(
                gson.toJson(
                    Config.RecallEvent(
                        2,
                        this.authorId,
                        this.operator!!.id,
                        this.messageIds.map { it.toString() }.toTypedArray().contentToString(),
                        this.messageInternalIds.map { it.toString() }.toTypedArray().contentToString(),
                        this.messageTime,
                        this.group.id,
                        this.bot.id
                    )
                )
            )

        }
        globalEventChannel.subscribeAlways<BotJoinGroupEvent.Invite>{
            cpp.Event(
                gson.toJson(
                    Config.BotJoinGroup(
                        1,
                        Config.Contact(2, this.group.id, 0, this.group.name, this.bot.id),
                        this.invitor.id
                    )
                )
            )
        }
        globalEventChannel.subscribeAlways<BotJoinGroupEvent.Active>{
            cpp.Event(
                gson.toJson(
                    Config.BotJoinGroup(
                        2,
                        Config.Contact(2, this.group.id, 0, this.group.name, this.bot.id),
                        0
                    )
                )
            )
        }
        globalEventChannel.subscribeAlways<BotJoinGroupEvent.Retrieve>{
            cpp.Event(
                gson.toJson(
                    Config.BotJoinGroup(
                        3,
                        Config.Contact(2, this.group.id, 0, this.group.name, this.bot.id),
                        0
                    )
                )
            )
        }
        globalEventChannel.subscribeAlways<BotInvitedJoinGroupRequestEvent> {
            //自动同意加群申请
            cpp.Event(
                gson.toJson(
                    Config.GroupInvite(
                        Config.GroupInviteSource(
                            this.bot.id,
                            this.eventId,
                            this.invitorId,
                            this.groupId,
                            this.groupName,
                            this.invitorNick
                        )
                    )
                )
            )
        }
        globalEventChannel.subscribeAlways<GroupTempMessageEvent> {
            //群临时会话
            cpp.Event(
                gson.toJson(
                    Config.GroupTempMessage(
                        Config.Contact(2, this.group.id, 0, this.group.name, this.bot.id),
                        Config.Contact(3, this.sender.id, this.group.id, this.sender.nameCardOrNick, this.bot.id),
                        this.message.serializeToMiraiCode(),
                        json.encodeToString(
                            MessageSource.Serializer,
                            this.source
                        )
                    )
                )
            )
        }

    }
}
fun CheckUpdate(){
    val tag = JSONObject(URL("https://api.github.com/repos/Nambers/MiraiCP/releases/latest").readText()).getString("tag_name")
    if(tag != now_tag)println("有最新可用版:$tag，前往:https://github.com/Nambers/MiraiCP/releases/latest下载")
}

@MiraiExperimentalApi
@MiraiInternalApi
fun main(args: Array<String>){
    // qqid, passworld, dllpath, checkupdate
    if(args.size != 3 && args.size != 4){
        println("参数不足或多余，请提供[qq账号 - long, qq密码 - string, dll存放位置 - string] 以及可选的[是否检测有没有可用升级 - 1 或 不填]")
        return
    }
    println("正在启动\n机器人qqid:${args[0]}\n机器人qq密码:${args[1]}\nc++部分dll存放地址${args[2]}")
    if(args.size == 4 && args[3] == "1"){
        CheckUpdate()
    }
    runBlocking {
        try {
            KotlinMain.main(valueOf(args[0]), args[1], args[2])
        }catch (e:NumberFormatException){
            println("${args[0]}不是一个有效的qq号数字")
            return@runBlocking
        }
    }
}
