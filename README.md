# 欢迎来到MiraiCP
[![License](https://img.shields.io/github/license/Nambers/MiraiCP)](https://github.com/Nambers/MiraiCP/blob/master/LICENSE)  [![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/Nambers/MiraiCP?include_prereleases)](https://github.com/Nambers/MiraiCP/releases/) [![GitHub last commit](https://img.shields.io/github/last-commit/Nambers/MiraiCP)](https://github.com/Nambers/MiraiCP/commit/master) ![Lines of code](https://img.shields.io/tokei/lines/github/Nambers/Miraicp)

>[中文](https://github.com/Nambers/MiraiCP/blob/master/README.md)
>
>[English](https://github.com/Nambers/MiraiCP/blob/master/README_en.md)


* [欢迎来到MiraiCP](#欢迎来到miraicp)
* [使用声明](#使用声明)
* [关于MiraiCP](#关于MiraiCP)
  * [使用流程:](#使用流程)
    * [1 配置环境](#1-配置环境)
    * [2 注意事项](#2-注意事项)
    * [3 使用方法:](#3-使用方法)
      * [3\.1 MiraiCP-Plugin插件配合mcl使用](#31-MiraiCP-Plugin插件配合mcl使用)
      * [3\.2 使用MiraiCP-Loader整合包](#32-使用MiraiCP-Loader整合包)
* [更新方式](#更新方式)
* [TODO](#todo)
* [许可](#许可)
> **Tips~**
> 如有意向一起开发本项目，请联系我邮箱(`1930893235@qq.com`) (￣▽￣)"
# 使用声明

0. MiraiCP是一个[Mirai](https://github.com/mamoe/mirai) 的C++语言的社区SDK，基于Mirai-console和Mirai-core插件模板开发

1. 本项目仅供学习参考，禁止用于任何商业用途(根据Mirai的AGPLv3许可协议开源)。

2. 本项目不含有任何旨在破坏用户计算机数据和获取用户隐私的恶意代码，不含有任何跟踪、监视用户计算机功能代码，不会收集任何用户个人信息，不会泄露用户隐私。

3. 本项目不提供任何具体功能实现，仅仅只是对项目mirai-console和mirai-core(详见[mirai仓库](https://github.com/mamoe/mirai))的二次封装。

4. 任何单位或个人认为本项目可能涉嫌侵权，应及时提出反馈，本项目将会第一时间对违规内容给予删除等相关处理。

# 关于MiraiCP

> 从v2.6.3-RC开始，使用utf8作为编码
> 
> vs需要加/utf8编译参数，见[微软文档](https://docs.microsoft.com/zh-cn/cpp/build/reference/utf-8-set-source-and-executable-character-sets-to-utf-8?view=msvc-160&viewFallbackFrom=vs-2017)
> 
> cmake方式已经加了以utf8编译不用改

<details>
<summary>支持的事件</summary>

这些内容可以在[Config.kt](https://github.com/Nambers/MiraiCP/blob/master/loader/src/main/kotlin/Config.kt)看到
  
| 事件名称     | 函数名称              |
|----------|-----------------------|
| 群聊消息     | GroupMessageEvent     |
| 私聊消息     | PrivateMessageEvent   |
| 好友申请     | NewFriendRequestEvent |
| 群聊邀请     | GroupInviteEvent      |
| 新群成员加入 | MemberJoinEvent       |
| 群成员离开   | MemberLeaveEvent      |
| 消息撤回     | RecallEvent           |
| 群临时会话   | GroupTempMessageEvent |
| 定时事件执行 | SchedulingEvent       |

</details>

**[在线API文档(包含示例)](https://eritque-arcus.tech/MiraiCP/html/)**

代码示例 [example.md](https://github.com/Nambers/MiraiCP/blob/master/doc/example.md)

本项目设计流程结构 [intro.md](https://github.com/Nambers/MiraiCP/blob/master/doc/intro.md)

Mirai支持的qq表情(对应miraicode的face)对应序号 [faces.md](https://github.com/Nambers/MiraiCP/blob/master/doc/faces.md)

# 使用流程:

## 1 配置环境
mirai需要java环境 **>=11**

## 2 注意事项

> MiraiCP版本规则: 从2.2.0开始 *(2021/1/31)*,版本前两位为Mirai的版本，后一位为SDK更新迭代版本

1. 目前只确定win下可用，其他操作系统未测试，理论上liunx应该可用，生成so文件替换dll文件即可

2. 如果vs报错找不到jni.h，把cpp/include文件夹加入到vs的库里面去(项目->属性->C++->常规)，include文件夹里包含了jni.h以及他的依赖文件

## 3 使用方法:
以下2种选择任意一种
### 3.1 MiraiCP-Plugin插件配合mcl使用
<details>
<summary>展开</summary>
	
0. 首先下载启动器(mcl), 下载地址 -> [官方](https://github.com/iTXTech/mirai-console-loader/)
1. 下载release中MiraiCP-Plugin.7z文件, 最新版([![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/Nambers/MiraiCP?include_prereleases)](https://github.com/Nambers/MiraiCP/releases/))
2. 复制压缩包根目录下的`.jar`文件留着下面有用(配套插件)
3. 打开`cpp`文件夹下的.sln文件从而打开vs的c++项目，或者用其他方式打开位于cpp文件夹下的c++项目
4. 在`procession.cpp`里自定义你自己的代码
5. 生成.dll文件
6. 留意`cpp/x64/Release/`(如果是vs x64 release生成)，`cpp/x64/Debug`(vs x64 debug生成)，`cpp/out`(vs cmake 生成)，`cpp/camke-debug-build`(clion cmake debug生成)这个路径下的`.dll`文件，留着下面有用
7. 打开上面下载的mcl文件夹
8. 把`.jar`文件(也就是配套插件)拷贝进mcl的plugin文件夹下
9. 运行一次mcl，然后不管有无报错，不要登录，直接退出(目的是生成data路径)
10. 打开mcl目录下的`data/miraiCP`路径(可能名字随着mirai版本的迭代会更改，包含MiraiCP即可)，把上面的.dll文件复制进来
	**或**把.dll文件放到任意位置，然后在`data/miraiCP`(可能名字随着mirai版本的迭代会更改，包含MiraiCP即可)下创建一个`miraicp.txt`把.dll的绝对路径写进去并不要写其他东西
11. 运行mcl

</details>

### 3.2 使用MiraiCP-Loader整合包

<details>
<summary>展开</summary>
	
1. 下载release中的MiraiCP-Loader.7z, 最新版([![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/Nambers/MiraiCP?include_prereleases)](https://github.com/Nambers/MiraiCP/releases/))
2. 打开`cpp`文件夹下的.sln文件从而打开vs的c++项目，或者用其他方式打开位于cpp文件夹下的c++项目
3. 在`procession.cpp`里自定义你自己的代码
4. 生成.dll文件
5. 留意`cpp/x64/Release/`(如果是vs x64 release生成)，`cpp/x64/Debug`(vs x64 debug生成)，`cpp/out`(vs cmake 生成)，`cpp/camke-debug-build`(clion cmake debug生成)这个路径下的`.dll`文件，留着下面有用
6. 更改run.bat文件里的启动参数，以\[qq号，密码和mirai-demo.dll路径(也就是上一步的.dll，可以复制出来，run.bat里可填相对路径或绝对路径),是否检测更新(可选，如果检查输入1否则不填)\]格式填写
7. 运行run.bat

</details>

**如果有其他问题，欢迎提交issue和提交PR贡献**

# 更新方式
1. 下载release包
2. 覆盖旧的`.mirai.jar`插件或者`.jar`loader
3. 把`cpp`文件夹下的全部单个文件覆盖(json和include文件夹不需要),主要为`pch.h`(预编译头文件),`pch.cpp`(dll入口点),`tools.h`(各种事件及对象类声明),`tools.cpp`(tools.h里的声明的实现),`constants.h`(常量表)
# TODO
查看[本项目的milestones里程碑](https://github.com/Nambers/MiraiCP/milestones)

# 许可
```
Copyright (C) 2020-2021 Eritque arcus and contributors.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or any later version(in your opinion).

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
```
