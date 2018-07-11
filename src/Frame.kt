import com.jme3.math.Vector2f
import javafx.scene.input.KeyCode
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Graphics
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JFrame
import javax.swing.JPanel

fun main(args: Array<String>) {
	val frame = JFrame("TRY")
	val panel=MyPanel()
	val transport=Transport(1f, Vector2f(200f,150f),maxAcceleration = 150f,maxVelocity = 125f,color = Color.BLACK)
	val state=ArriveState(transport,Vector2f(100f,150f),SpeedLevel.MIDDLE)
	transport.states.add(state)
	panel.transports.add(transport)
	val evader = Transport(1f, Vector2f(300f, 500f), maxAcceleration = 150f, maxVelocity = 50f,color = Color.BLUE)
	val state2 = EvadeState(evader, transport)
	evader.states.add(state2)
	panel.transports.add(evader)
	panel.addMouseListener(object:MouseAdapter(){
		override fun mouseClicked(e: MouseEvent?) {
			state.target= Vector2f(e!!.x.toFloat(),e.y.toFloat()).add(panel.axis.negate())
		}
	})
	frame.addKeyListener(object:KeyAdapter(){
		override fun keyPressed(e: KeyEvent?) {
			when(e!!.keyCode){
				KeyEvent.VK_A -> panel.a=true
				KeyEvent.VK_D -> panel.d=true
				KeyEvent.VK_W -> panel.w=true
				KeyEvent.VK_S -> panel.s=true
			}
		}
		
		override fun keyReleased(e: KeyEvent?) {
			when(e!!.keyCode){
				KeyEvent.VK_A -> panel.a=false
				KeyEvent.VK_D -> panel.d=false
				KeyEvent.VK_W -> panel.w=false
				KeyEvent.VK_S -> panel.s=false
			}
		}
	})
	frame.contentPane.add(panel,BorderLayout.CENTER)
	frame.isVisible=true
	frame.defaultCloseOperation=JFrame.EXIT_ON_CLOSE
	frame.setSize(1200, 800)
	val thread=object :Thread(){
		override fun run() {
			super.run()
			while(true) {
				panel.repaint()
				Thread.sleep(20)
			}
		}
	}
	thread.start()
}
class MyPanel:JPanel(){
	val transports=ArrayList<Transport>()
	var lstUpdate=System.currentTimeMillis()
	val axis=Vector2f(0f,0f)
	var a=false
	var d=false
	var w=false
	var s=false
	override fun paintComponent(g: Graphics?) {
		super.paintComponent(g)
		if(g!=null){
			val time=System.currentTimeMillis()
			val tpf=time-lstUpdate
			
			if(a) axis.addLocal(Vector2f(tpf.toFloat(),0f))
			if(d) axis.addLocal(Vector2f(-tpf.toFloat(), 0f))
			if(w) axis.addLocal(Vector2f(0f, tpf.toFloat()))
			if(s) axis.addLocal(Vector2f(0f,-tpf.toFloat()))
			
			
			g.color= Color.WHITE
			g.fillRect(0, 0, width, height)
			g.color= Color.BLACK
			lstUpdate=time
			for (transport in transports) {
				transport.update(tpf)
				transport.draw(g,axis)
			}
			g.color=Color.RED
			val v=(transports[0].states[0] as ArriveState).target.add(axis)
			g.drawOval(v.x.toInt() - 2, v.y.toInt() - 2, 4, 4)
			g.color= Color.GREEN
			val v2=(transports[1].states[0] as EvadeState).evadeTarget.add(axis)
			g.drawOval(v2.x.toInt() - 2, v2.y.toInt() - 2, 4, 4)
		}
	}
}