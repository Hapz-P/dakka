package com.mygdx.game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.input.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.*;
import com.sun.tools.javac.comp.Todo;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

import java.awt.event.MouseEvent;

public class MyGdxGame extends ApplicationAdapter {
	public Model arena_model;
	public ModelInstance arena;
	public ModelBatch modelBatch;
	public Environment environment;
	public FirstPersonCameraController camController;
	public float delta;
	public ModelLoader loader = new ObjLoader();
	public Model bulletmodel;
	public ModelInstance bulletdraw;
	public float width;
	public float height;
	public class FirstPersonCameraController extends InputAdapter {
		// TODO: replace direction with function
		private final Camera camera;
		public final IntIntMap keys = new IntIntMap();
		private int FORWARD = Keys.W;
		private int STRAFE_LEFT = Keys.A;
		private int BACKWARD = Keys.S;
		private int STRAFE_RIGHT = Keys.D;
		private int UP = Keys.SPACE;
		private int DOWN = Keys.V;


		private float velocity = 5;
		private float degreesPerPixel = 0.5f;
		private final Vector3 tmp = new Vector3();

		public FirstPersonCameraController (Camera camera) {
			this.camera = camera;
		}

		@Override
		public boolean keyDown (int keycode) {
			keys.put(keycode, keycode);
			return true;
		}

		@Override
		public boolean keyUp (int keycode) {
			keys.remove(keycode, 0);
			return true;
		}

		/** Sets the velocity in units per second for moving forward, backward and strafing left/right.
		 * @param velocity the velocity in units per second */
		public void setVelocity (float velocity) {
			this.velocity = velocity;
		}

		/** Sets how many degrees to rotate per pixel the mouse moved.
		 * @param degreesPerPixel the degrees rotate per pixel */
		public void setDegreesPerPixel (float degreesPerPixel) {
			this.degreesPerPixel = degreesPerPixel;
		}

		@Override
		public boolean mouseMoved (int screenX, int screenY) {
			float deltaX = -Gdx.input.getDeltaX() * degreesPerPixel;
			float deltaY = -Gdx.input.getDeltaY() * degreesPerPixel;
			camera.direction.rotate(camera.up, deltaX).nor();
			if ((!(camera.direction.dot(camera.up) > 0.99d && deltaY > 0) &&
					!(camera.direction.dot(-camera.up.x, -camera.up.y, -camera.up.z) > 0.99d && deltaY < 0))) {
				tmp.set(camera.direction).crs(camera.up).nor();
				camera.direction.rotate(tmp, deltaY);
			}


// camera.up.rotate(tmp, deltaY);
			return true;
		}

		public void update () {
			update(Gdx.graphics.getDeltaTime());
		}

		public void update (float deltaTime) {
			if (keys.containsKey(FORWARD)) {
				tmp.set(camera.direction).nor().scl(deltaTime * velocity);
				camera.position.add(tmp);
			}
			if (keys.containsKey(BACKWARD)) {
				tmp.set(camera.direction).nor().scl(-deltaTime * velocity);
				camera.position.add(tmp);
			}
			if (keys.containsKey(STRAFE_LEFT)) {
				tmp.set(camera.direction).crs(camera.up).nor().scl(-deltaTime * velocity);
				camera.position.add(tmp);
			}
			if (keys.containsKey(STRAFE_RIGHT)) {
				tmp.set(camera.direction).crs(camera.up).nor().scl(deltaTime * velocity);
				camera.position.add(tmp);
			}
			if (keys.containsKey(UP)) {
				Vector3 tangent = camera.up.cpy().crs(camera.direction).nor();
				tmp.set(camera.direction).crs(tangent).nor().scl(deltaTime * velocity);
				camera.position.add(tmp);
			}
			if (keys.containsKey(DOWN)) {
				Vector3 tangent = camera.up.cpy().crs(camera.direction).nor();
				tmp.set(camera.direction).crs(tangent).nor().scl(-deltaTime * velocity);
				camera.position.add(tmp);
			}
			camera.update(true);
		}
	}

	public class Character {
		//Cam defines pos, dir, & other movement related vectors
		PerspectiveCamera view;
		//controller defines movement, speed, and... controls
		FirstPersonCameraController controller;
		float fireTimer = 0f; // timer for ROF
		float ROF = 70F; //how long till next bullet
		float fireSpeed = 15f; //speed in bullets per second (maybe)
		float aimSpeed = 15f;
		float spread = 15f; //radius speed
		/** TODO: rate of fire, dmg, hp, etc
		 TODO: add models, textures, music, etc
		 TODO: S T O R Y L I N E
		 TODO: velocity, friction, knockback resistance
		 TODO: animation.
		 TODO: ACTUAL CHARACTERS!!! */
	}

	public static class Bullet {
		Vector3 pos = new Vector3(0,0,0);
		Vector3 vel = new Vector3(0,0,0);

	}
	final Array<Bullet> bullets = new Array<Bullet>();
	
	Character player = new Character();
	float timer = 0f;
	@Override public void create() {
		//basic vars & setup
		float width = Gdx.graphics.getWidth();
		float height = Gdx.graphics.getHeight();
		Gdx.input.setCursorCatched(true);
		Gdx.input.setCursorPosition(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
		final float sensM = 6.2918f;
		//universal bullet model & instant
		bulletmodel = loader.loadModel(Gdx.files.internal("3d/basic_bullet/model.obj"));
		bulletdraw = new ModelInstance(bulletmodel);
		//chars have cams and controllers to simplify player & controls
		//"view" is char cams
		player.view = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		player.view.position.set(0,0,0);
		player.view.near = 0.25f;
		player.view.far = 150f;
		//player controller
		player.controller = new FirstPersonCameraController(player.view);
		player.controller.setDegreesPerPixel(1/sensM);
		player.controller.setVelocity(15);
		player.view.update();

		modelBatch = new ModelBatch();

		// TODO: add more arenas (aesthetic)

		arena_model = loader.loadModel(Gdx.files.internal("3d/arenas/basic/model.obj"));
		arena = new ModelInstance(arena_model);


		Gdx.input.setInputProcessor(player.controller);

	}

	public void update() {
		delta = Gdx.graphics.getDeltaTime();
		timer += delta;
		player.fireTimer -= delta;
		if( bullets.size < 10000 & Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) & player.fireTimer < 1){
			// TODO: fix bullet spaghetti
			Bullet newbul = new Bullet();
			bullets.add(newbul);
			//set position
			newbul.pos.set(player.view.position).mulAdd(player.view.direction,10);
			//hor direction
			Vector3 hor = (player.view.direction).cpy().crs(player.view.up).nor();
			//wiggle
			hor.set(hor.x*MathUtils.cos(timer),hor.y*MathUtils.cos(timer),hor.z*MathUtils.cos(timer));
			//wiggle vert
			Vector3 vert = new Vector3().mulAdd(player.view.up,MathUtils.sin(timer));
			//apply wiggly vel dir
			newbul.vel.set(player.view.direction).scl(player.aimSpeed).mulAdd(vert,player.spread).mulAdd(hor,player.spread).scl(delta).nor().scl(player.fireSpeed);
			//sped = bullets.get(bullets.size-1).vel.set(player.view.direction).scl(sped).add(vert).add(hor).scl(delta).scl(player.ROF).len();
			//set time until next bullet
			player.fireTimer = player.ROF*delta;
		}
		if(player.fireTimer > player.ROF*delta){player.fireTimer = player.ROF*delta;}
		if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) {
			Gdx.app.exit();
		}
		player.view.update();
		player.controller.update(delta);
		for(int i = bullets.size-1; i >= 0; i--){
			if(bullets.get(i).pos.dst(player.view.position) < player.view.far)
			{ bullets.get(i).pos.mulAdd(bullets.get(i).vel,delta); } else
				{bullets.removeIndex(i);} // TODO: bullet erasure

		}
		Gdx.app.log("#", "bullets:" + bullets.size);
		Gdx.app.log("#", "ROF:" + player.ROF*delta);
	}


	@Override public void render() {
		// TODO: shaders, lighting, doom aura, GUI
		//update before draw, just for ease of use
		update();
		//CLEAR
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		//sight
		modelBatch.begin(player.view);
		modelBatch.render(arena);
		for(int i = 0; i < bullets.size; i++){
			//method for each bullet, given universal instance & model
			if(bullets.get(i).pos.dst(player.view.position) < player.view.far) {
				bulletdraw.transform.setToTranslation(bullets.get(i).pos); //move instant
				modelBatch.render(bulletdraw); //draw
			}
		} //repeat
		modelBatch.end();
	}

	@Override
	//TODO: throw away your trash, peasant
	public void dispose() {
		modelBatch.dispose();
		arena_model.dispose();
		bulletmodel.dispose();
	}




}
