package com.xulog.frpc_gui

import javafx.application.Application
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.TextArea
import javafx.scene.layout.Pane
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.StartedProcess
import org.zeroturnaround.exec.stream.LogOutputStream
import org.zeroturnaround.process.Processes
import java.awt.TrayIcon
import java.io.File
import javax.imageio.ImageIO
import kotlin.properties.Delegates


class App : Application() {

    val logArea = TextArea()
    val btn = Button("启动")
    var process: StartedProcess? = null
    val runtimePath: File = File(".")

    init {
        val frpcExe = File(runtimePath, "frpc.exe")
        val frpcIni = File(runtimePath, "frpc_min.ini")
        if (!frpcExe.exists() || !frpcIni.exists()) {
            btn.visibleProperty().set(false)
            logArea.text = "${frpcExe.absolutePath} 或者 ${frpcIni.absolutePath} 不存在"
        } else {
            stopFrpc()
        }
        logArea.isEditable = false
        logArea.isWrapText = true
        logArea.prefWidth = 300.0
        logArea.prefHeight = 200.0
    }

    var isRunning: Boolean by Delegates.observable(findFrpcPid().isNotEmpty(), { property, oldValue, newValue ->
        if (newValue) {
            startFrpc()
            btn.text = "关闭"
        } else {
            stopFrpc()
            btn.text = "启动"
        }
    })

    override fun start(primaryStage: Stage) {
        // Initializing the StackPane class
        val root = Pane()

        btn.setOnAction { isRunning = !isRunning }
        // Adding all the nodes to the FlowPane
        root.children.add(logArea)
        root.children.add(btn)
        //Creating a scene object
        val scene = Scene(root, 600.0, 360.0)
        logArea.layoutX = 9.0
        logArea.layoutY = 14.0
        logArea.prefWidth = 582.0
        logArea.prefHeight = 292.0
        btn.layoutY = 323.0
        btn.layoutX = 273.0
        btn.alignment = Pos.BOTTOM_CENTER
        primaryStage.isResizable = false
        primaryStage.title = "frpc_gui v0.1"
        primaryStage.scene = scene
        // show the window(primaryStage)


        val tray = java.awt.SystemTray.getSystemTray()

//        val resource: URL = this.javaClass.classLoader.getResource("pivotal-tracker-fluid-icon-2013.png")
        val image = ImageIO.read(this.javaClass.classLoader.getResource("pivotal-tracker-fluid-icon-2013.png"))
        val trayIcon = TrayIcon(image, "WTF")
        trayIcon.isImageAutoSize = true
        Platform.setImplicitExit(false);

        trayIcon.addActionListener {
            Platform.runLater({
                if (primaryStage.isShowing) {
                    primaryStage.hide()
                } else {
                    primaryStage.show()
//                    primaryStage.
                    primaryStage.toFront()
                }
            })

        }
        tray.add(trayIcon)
        primaryStage.initStyle(StageStyle.UTILITY);

        primaryStage.show()

//        primaryStage.iconifiedProperty().addListener({ observableValue, t1, t2 ->
//            if (t2) {
//                //最小化
//                    primaryStage.hide()
//            }else{
//                //最小化
//                    primaryStage.show()
//            }
//        });

        primaryStage.onCloseRequest = EventHandler {
            Platform.exit()
            tray.remove(trayIcon)
        }
    }


    fun startFrpc() {
        val command = ProcessExecutor().command(File(runtimePath, "frpc.exe").absolutePath, "-c", File(runtimePath, "frpc_min.ini").absolutePath)
        process = command.redirectOutput(object : LogOutputStream() {
            override fun processLine(line: String?) {
                val str = (logArea.text + "\n" + line).split("\n")
                logArea.text = str.takeLast(100).joinToString("\n")
            }
        }).start()
    }

    fun stopFrpc() {
        if (process != null) {
            process!!.process.destroyForcibly()
            process = null
        } else {
            val pid = findFrpcPid()
            if (pid.isNotEmpty()) {
                pid.forEach {
                    Processes.newPidProcess(it).destroyForcefully()
                }
            }
        }
        logArea.text = ""
    }

    fun findFrpcPid(): List<Int> {
        val execute = ProcessExecutor().command("tasklist", "/nh").readOutput(true).execute()
        val map = execute.output.lines.map { it.split(Regex("\\s{1,}")) }
        val mapValues = map.groupBy { it[0] }.mapValues { it.value.map { it.getOrElse(1, { "?" }) } }
        return mapValues.getOrElse("frpc.exe", { emptyList() }).map(String::toInt)
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            launch(App::class.java, *args)
        }
    }

}