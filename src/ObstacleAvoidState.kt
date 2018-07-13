import com.jme3.math.Vector2f
import java.awt.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

class ObstacleAvoidState(transport: Transport, val list: ArrayList<Obstacle>) : State(transport) {
	override fun update(): Vector2f {
		var localList = ArrayList<Obstacle>()
		for (i in list) {
			localList.add(Obstacle(i.location.toLocalAxis(), i.radius))
		}
		localList = ArrayList(localList.filter { it.location.x+it.radius> 0 && it.location.x-it.radius < transport.velocity.length() && abs(it.location.y) < it.radius + transport.size })
		val min = localList.minBy { it.location.x - sqrt((it.radius+transport.size) * (it.radius+transport.size) - it.location.y * it.location.y) }
				?: return Vector2f(0f, 0f)
		var multiplier = 2f - (min.location.x-sqrt((min.radius+transport.size)*(min.radius+transport.size)-min.location.y*min.location.y))/(transport.velocity.length())
		multiplier= max(1f,multiplier)
		val force = Vector2f(0f, 0f)
		force.y = (min.radius + transport.size - min.location.y) * multiplier*2
		if(min.location.y>0)force.y=-force.y
		force.x-(min.radius-min.location.x)*0.1f
		val answer = Vector2f(0f, 0f)
		answer.addLocal(transport.velocity.normalize().mult(force.x))
		answer.addLocal(Vector2f(transport.velocity.normalize().y, -transport.velocity.normalize().x).mult(force.y))
		return answer.mult(1/9.3f)
	}
	
	fun Vector2f.toLocalAxis(): Vector2f {
		val v = Vector2f()
		v.x = this.add(transport.location.negate()).dot(transport.velocity.normalize())
		v.y = this.add(transport.location.negate()).dot(Vector2f(transport.velocity.normalize().y, -transport.velocity.normalize().x))
		return v
	}
}

data class Obstacle(var location: Vector2f, var radius: Float, var color: Color = Color.GRAY)