import com.jme3.math.Vector2f
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Graphics
import java.awt.event.*
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.math.max

fun main(args: Array<String>) {
	val frame = JFrame("TRY")
	val list = makeObstacles()
	val panel = MyPanel(list)
	val transport = Transport(1f, Vector2f(200f, 150f), maxAcceleration = 200f, maxVelocity = 150f, color = Color.BLACK)
	val state = ArriveState(transport, Vector2f(0f, 0f), SpeedLevel.MIDDLE)
	transport.states.add(state)
	transport.states.add(ObstacleAvoidState(transport, list))
	panel.transports.add(transport)
	
	val newList = ArrayList<Transport>()
	repeat(50) {
		val new = Transport(1f, Vector2f(1200f * Math.random().toFloat(), 800f * Math.random().toFloat()), maxAcceleration = 200f, maxVelocity = 150f, color = Color.GREEN)
		val newState = SeparationState(new, newList)
		new.states.add(newState)
		new.states.add(AlignmentState(new, newList))
		new.states.add(CohesionState(new, newList))
		new.states.add(EvadeState(new, transport))
		new.states.add(ObstacleAvoidState(new, list))
		panel.transports.add(new)
		newList.add(new)
	}
	var selected = newList[0]
	var lstColor = selected.color
	panel.addMouseListener(object : MouseAdapter() {
		override fun mouseClicked(e: MouseEvent?) {
			state.target = Vector2f(e!!.x.toFloat(), e.y.toFloat()).add(panel.axis.negate())
		}
		
	})
	panel.addMouseMotionListener(object : MouseMotionAdapter() {
		override fun mouseMoved(e: MouseEvent?) {
			selected.color = lstColor
			selected = newList.minBy { it.location.distanceSquared(Vector2f(e!!.x.toFloat(), e.y.toFloat()).add(panel.axis.negate())) }!!
			lstColor = selected.color
			selected.color = Color.RED
		}
	})
	frame.addKeyListener(object : KeyAdapter() {
		var cd = false
		override fun keyPressed(e: KeyEvent?) {
			when (e!!.keyCode) {
				KeyEvent.VK_A -> panel.a = true
				KeyEvent.VK_D -> panel.d = true
				KeyEvent.VK_W -> panel.w = true
				KeyEvent.VK_S -> panel.s = true
				KeyEvent.VK_SPACE -> {
					if (!cd) {
						val bullet = Transport(1f, Vector2f(transport.location), maxVelocity = 200f, maxAcceleration = 300f, color = Color.BLUE, size = 3f)
						bullet.touchable = false
						bullet.velocity = Vector2f(transport.velocity)
						bullet.states.add(ObstacleAvoidState(bullet, list))
						bullet.states.add(PursuitState(bullet, selected))
						bullet.states.add(object : State(bullet) {
							override fun update(): Vector2f {
								bullet.maxAcceleration = max(bullet.maxAcceleration - 2f, 50f)
								return Vector2f(0f, 0f)
							}
						})
						selected.states.add(EvadeState(selected, bullet))
						panel.transports.add(bullet)
						val thread = object : Thread() {
							override fun run() {
								cd = true
								Thread.sleep(300)
								bullet.touchable=true
								Thread.sleep(700)
								cd = false
								Thread.sleep(9000)
								bullet.dead = true
							}
						}
						thread.start()
					}
				}
			}
		}
		
		override fun keyReleased(e: KeyEvent?) {
			when (e!!.keyCode) {
				KeyEvent.VK_A -> panel.a = false
				KeyEvent.VK_D -> panel.d = false
				KeyEvent.VK_W -> panel.w = false
				KeyEvent.VK_S -> panel.s = false
			}
		}
	})
	frame.contentPane.add(panel, BorderLayout.CENTER)
	frame.isVisible = true
	frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
	frame.setSize(1200, 800)
	val thread = object : Thread() {
		override fun run() {
			super.run()
			while (true) {
				panel.repaint()
				Thread.sleep(20)
			}
		}
	}
	thread.start()
	val battleStart=object :Thread(){
		override fun run() {
			Thread.sleep(2000)
			for(i in panel.transports){
				i.touchable=true
			}
		}
	}
	battleStart.start()
}

fun makeObstacles(): ArrayList<Obstacle> {
	val list = ArrayList<Obstacle>()
	fun makeObstacle(): Obstacle = Obstacle(Vector2f((Math.random() * 1200).toFloat(), (Math.random() * 800).toFloat()), Math.random().toFloat() * 100)
	repeat(10) { list.add(makeObstacle()) }
	return list
}

fun explosion(panel: MyPanel) {
	val explosionThread = object : Thread() {
		override fun run() {
			repeat(3) {
				panel.axis.x += 4
				panel.axis.y += 4
				Thread.sleep(20)
			}
			repeat(3) {
				panel.axis.x -= 4
				panel.axis.y -= 4
				Thread.sleep(20)
			}
			repeat(3) {
				panel.axis.x -= 4
				panel.axis.y -= 4
				Thread.sleep(20)
			}
			repeat(3) {
				panel.axis.x += 4
				panel.axis.y += 4
				Thread.sleep(20)
			}
		}
	}
	explosionThread.start()
	
}

class MyPanel(val list: ArrayList<Obstacle>) : JPanel() {
	val transports = ArrayList<Transport>()
	var lstUpdate = System.currentTimeMillis()
	val axis = Vector2f(0f, 0f)
	var a = false
	var d = false
	var w = false
	var s = false
	override fun paintComponent(g: Graphics?) {
		if (g != null) {
			val time = System.currentTimeMillis()
			val tpf = time - lstUpdate
			
			if (a) axis.addLocal(Vector2f(tpf.toFloat(), 0f))
			if (d) axis.addLocal(Vector2f(-tpf.toFloat(), 0f))
			if (w) axis.addLocal(Vector2f(0f, tpf.toFloat()))
			if (s) axis.addLocal(Vector2f(0f, -tpf.toFloat()))
			
			
			g.color = Color.WHITE
			g.fillRect(0, 0, width, height)
			g.color = Color.GRAY
			for (i in list) {
				val location = i.location.add(axis)
				g.color = i.color
				g.fillOval((location.x - i.radius).toInt(), (location.y - i.radius).toInt(), i.radius.toInt() * 2, i.radius.toInt() * 2)
			}
			g.color = Color.BLACK
			lstUpdate = time
			for (transport in transports) {
				transport.update(tpf)
				transport.draw(g, axis)
				for (obstacle in list) {
					if (transport.location.distance(obstacle.location) < 2f + obstacle.radius) {
						transport.dead = true
						explosion(this)
					}
				}
			}
			for (i in 0 until transports.size) {
				if (transports[i].touchable)
					for (j in i + 1 until transports.size) {
						if (transports[j].touchable)
							if (transports[i].location.distanceSquared(transports[j].location) < 25f) {
								transports[i].dead = true
								transports[j].dead = true
								explosion(this)
							}
					}
			}
			var i = 0
			while (i < transports.size) {
				if (transports[i].dead) {
					transports.removeAt(i)
				}
				i++
			}
			if (transports[0].states[0] is ArriveState) {
				g.color = Color.RED
				val v = (transports[0].states[0] as ArriveState).target.add(axis)
				g.drawOval(v.x.toInt() - 2, v.y.toInt() - 2, 4, 4)
			} else {
				System.exit(0)
			}
			/*g.color= Color.GREEN
			val v2=(transports[1].states[0] as PursuitState).seekTarget.add(axis)
			g.drawOval(v2.x.toInt() - 2, v2.y.toInt() - 2, 4, 4)*/
		}
	}
}