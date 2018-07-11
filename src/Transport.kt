import com.jme3.math.Vector2f
import java.awt.Color
import java.awt.Graphics

open class Transport(
		private val mass: Float,
		val location: Vector2f,
		val maxVelocity: Float = 50f,
		val maxAcceleration: Float = 20f,
		val color: Color=Color.BLACK
) {
	var velocity = Vector2f(0.00001f, 0f)
	var acceleration = Vector2f(0f, 0f)
	var heading=Vector2f(1f,0f)
	val states = ArrayList<State>()
	fun update(tpf: Long) {
		val force = Vector2f(0f, 0f)
		for (state in states) {
			force.addLocal(state.update())
		}
		acceleration.addLocal(force.mult(1/mass))
		acceleration.toMaxLocal(maxAcceleration)
		acceleration.multLocal(tpf/1000f)
		velocity.addLocal(acceleration.multLocal(1 / mass))
		velocity.toMaxLocal(maxVelocity)
		if (velocity.lengthSquared() > 5f) {
			heading=velocity.normalize()
		}
		location.addLocal(velocity.mult((tpf/1000f)))
	}
	fun draw(g:Graphics,axis:Vector2f){
		g.color=color
		val x=heading.normalize()
		val y = Vector2f(-x.y, x.x)
		val p1=x.mult(8f).add(location).add(axis)
		val p2 = x.negate().add(y).mult(5f).add(location).add(axis)
		val p3 = x.negate().add(y.negate()).mult(5f).add(location).add(axis)
		fun drawLine(p1:Vector2f,p2:Vector2f,g: Graphics){
			g.drawLine(p1.x.toInt(),p1.y.toInt(),p2.x.toInt(),p2.y.toInt())
		}
		drawLine(p1, p2, g)
		drawLine(p1, p3, g)
		drawLine(p2, p3, g)
	}
}

fun Vector2f.toMax(max: Float): Vector2f {
	val vector2f = Vector2f(this)
	return if (vector2f.length() > max) {
		vector2f.normalizeLocal().multLocal(max)
	} else {
		vector2f
	}
}

fun Vector2f.toMaxLocal(max: Float): Vector2f {
	return if (length() > max) {
		normalizeLocal().multLocal(max)
	} else this
}