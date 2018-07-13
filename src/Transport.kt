import com.jme3.math.Vector2f
import java.awt.Color
import java.awt.Graphics
import kotlin.math.cos
import kotlin.math.sqrt

open class Transport(
		private val mass: Float,
		val location: Vector2f,
		val maxVelocity: Float = 50f,
		val maxAcceleration: Float = 20f,
		var color: Color = Color.BLACK,
		val size: Float = 5f
) {
	var velocity = Vector2f(0.00001f, 0f)
	var acceleration = Vector2f(0f, 0f)
	var heading = Vector2f(1f, 0f)
	val states = ArrayList<State>()
	fun update(tpf: Long) {
		val firstLevelForce = Vector2f(0f, 0f)
		val secondLevelForce = Vector2f(0f, 0f)
		var force = Vector2f(0f, 0f)
		for (state in states) {
			val simpleForce = state.update()
			when (state) {
				is AlignmentState -> secondLevelForce.addLocal(simpleForce)
				is ArriveState -> secondLevelForce.addLocal(simpleForce.mult(150f))
				is CohesionState -> secondLevelForce.addLocal(simpleForce.mult(150f))
				is EvadeState -> secondLevelForce.addLocal(simpleForce.mult(150f))
				is FleeState -> secondLevelForce.addLocal(simpleForce.mult(150f))
				is ObstacleAvoidState -> firstLevelForce.addLocal(simpleForce.mult(50f))
				is PursuitState -> secondLevelForce.addLocal(simpleForce.mult(150f))
				is SeekState -> secondLevelForce.addLocal(simpleForce.mult(150f))
				is SeparationState -> firstLevelForce.addLocal(simpleForce.mult(150f))
				is WanderState -> secondLevelForce.addLocal(simpleForce.mult(150f))
			}
		}
		if (firstLevelForce.length() > maxAcceleration) {
			force = firstLevelForce.normalize().mult(maxAcceleration)
		} else {
			val l = firstLevelForce.length()
			val a = maxAcceleration
			val cos = cos(firstLevelForce.angleBetween(secondLevelForce))
			val x = (2 * l * cos + sqrt(4 * l * l * cos * cos - 4 * l * l + 4 * a * a)) / 2
			force = firstLevelForce.add(secondLevelForce.normalize().mult(x))
		}
		acceleration.addLocal(force.mult(1 / mass))
		acceleration.toMaxLocal(maxAcceleration)
		acceleration.multLocal(tpf / 1000f)
		velocity.addLocal(acceleration.multLocal(1 / mass))
		velocity.toMaxLocal(maxVelocity)
		if (velocity.lengthSquared() > 5f) {
			heading = velocity.normalize()
		}
		location.addLocal(velocity.mult((tpf / 1000f)))
	}
	
	fun draw(g: Graphics, axis: Vector2f) {
		g.color = color
		val x = heading.normalize()
		val y = Vector2f(-x.y, x.x)
		val p1 = x.mult(size * 1.5f).add(location).add(axis)
		val p2 = x.negate().add(y).mult(size).add(location).add(axis)
		val p3 = x.negate().add(y.negate()).mult(size).add(location).add(axis)
		fun drawLine(p1: Vector2f, p2: Vector2f, g: Graphics) {
			g.drawLine(p1.x.toInt(), p1.y.toInt(), p2.x.toInt(), p2.y.toInt())
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