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
	
	var newList = ArrayList<Transport>()
	repeat(50) {
		val new = Transport(1f, Vector2f(3600f * Math.random().toFloat() - 1200f, 2400f * Math.random().toFloat() - 800f), maxAcceleration = 200f, maxVelocity = 150f, color = Color.GREEN)
		val newState = SeparationState(new, newList)
		new.states.add(newState)
		new.states.add(AlignmentState(new, newList))
		new.states.add(CohesionState(new, newList))
		new.states.add(EvadeState(new, transport))
		new.states.add(ObstacleAvoidState(new, list))
		new.states.add(WanderState(new))
		panel.transports.add(new)
		newList.add(new)
	}
	var mouseLocation = Vector2f(0f, 0f)
	var selected = newList[0]
	var lstColor = selected.color
	panel.addMouseListener(object : MouseAdapter() {
		override fun mouseClicked(e: MouseEvent?) {
			state.target = Vector2f(e!!.x.toFloat(), e.y.toFloat()).add(panel.axis.negate())
		}
		
	})
	panel.addMouseMotionListener(object : MouseMotionAdapter() {
		override fun mouseMoved(e: MouseEvent?) {
			mouseLocation = Vector2f(e!!.x.toFloat(), e.y.toFloat())
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
						val bullet = Transport(1f, Vector2f(transport.location), maxVelocity = 200f, maxAcceleration = 300f, color = Color.BLUE, size = 3f,explode = false)
						bullet.touchable = false
						bullet.velocity = Vector2f(transport.velocity)
						bullet.states.add(ObstacleAvoidState(bullet, list))
						bullet.states.add(PursuitState(bullet, selected))
						bullet.states.add(object : State(bullet) {
							override fun update(): Vector2f {
								bullet.maxAcceleration = max(bullet.maxAcceleration - 2f, 20f)
								return Vector2f(0f, 0f)
							}
						})
						selected.states.add(EvadeState(selected, bullet))
						panel.transports.add(bullet)
						val thread = object : Thread() {
							override fun run() {
								cd = true
								Thread.sleep(500)
								bullet.touchable = true
								Thread.sleep(500)
								cd = false
								Thread.sleep(9000)
								bullet.dead = true
							}
						}
						thread.start()
					}
				}
				KeyEvent.VK_Z -> {
					if (!cd) {
						val bullet = object : Transport(1f, Vector2f(transport.location), maxVelocity = 10000f, maxAcceleration = 0f, color = Color.ORANGE, size = 2f,explode = false) {
							override fun draw(g: Graphics, axis: Vector2f) {
								val p1 = location.add(velocity.normalize().mult(size)).add(axis)
								val p2 = location.add(velocity.normalize().mult(size).negate()).add(axis)
								drawLine(p1, p2, g)
							}
						}
						bullet.velocity = transport.velocity.add(transport.velocity.normalize().mult(400f))
						panel.transports.add(bullet)
						val thread = object : Thread() {
							override fun run() {
								cd = true
								Thread.sleep(250)
								bullet.touchable = true
								cd = false
								Thread.sleep(4750)
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
				selected.color = lstColor
				newList = ArrayList(panel.transports.filter { it.color == Color.GREEN })
				selected = newList.minBy { it.location.distanceSquared(mouseLocation.add(panel.axis.negate())) }!!
				lstColor = selected.color
				selected.color = Color.RED
				panel.repaint()
				Thread.sleep(20)
			}
		}
	}
	thread.start()
	val battleStart = object : Thread() {
		override fun run() {
			Thread.sleep(2000)
			for (i in panel.transports) {
				i.touchable = true
			}
		}
	}
	battleStart.start()
}

fun makeObstacles(): ArrayList<Obstacle> {
	val list = ArrayList<Obstacle>()
	fun makeObstacle(): Obstacle = Obstacle(Vector2f((Math.random() * 3600).toFloat() - 1200f, (Math.random() * 2400).toFloat() - 800f), Math.random().toFloat() * 100)
	repeat(30) { list.add(makeObstacle()) }
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
	var start = true
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
			g.fillRect(-1200 + axis.x.toInt() - 50, -800 + axis.y.toInt() - 50, 3700, 50)
			g.fillRect(-1200 + axis.x.toInt() - 50, 1600 + axis.y.toInt(), 3700, 50)
			g.fillRect(-1200 + axis.x.toInt() - 50, -800 + axis.y.toInt() - 50, 50, 2500)
			g.fillRect(2400 + axis.x.toInt(), -800 + axis.y.toInt() - 50, 50, 2500)
			g.color = Color.BLACK
			lstUpdate = time
			for (transport in transports) {
				if (start)
					transport.update(tpf)
				transport.draw(g, axis)
				if (transport.location.x < -1200 || transport.location.x > 2400 || transport.location.y < -800 || transport.location.y > 1600) {
					transport.dead = true
					if (transport.explode)
						explosion(this)
				}
				for (obstacle in list) {
					if (transport.location.distance(obstacle.location) < 2f + obstacle.radius) {
						transport.dead = true
						if (transport.explode)
							explosion(this)
					}
				}
			}
			for (i in 0 until transports.size) {
				if (transports[i].touchable)
					for (j in i + 1 until transports.size) {
						if (transports[j].touchable)
							if (transports[i].location.distanceSquared(transports[j].location) < 50f) {
								transports[i].dead = true
								transports[j].dead = true
								if (transports[i].explode || transports[j].explode)
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
				if (start) {
					start = false
					repeat(5) { explosion(this) }
				}
			}
			/*g.color= Color.GREEN
			val v2=(transports[1].states[0] as PursuitState).seekTarget.add(axis)
			g.drawOval(v2.x.toInt() - 2, v2.y.toInt() - 2, 4, 4)*/
		}
	}
}