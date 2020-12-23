package com.mygdx.game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
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
import net.mgsx.gltf.scene3d.scene.SceneManager;

import java.awt.event.MouseEvent;

public class MyGdxGame extends ApplicationAdapter {
	public Model arena_model;
	public ModelInstance arena;
	public ModelBatch modelBatch;
	public SpriteBatch spriteBatch;
	public Environment environment;
	public FirstPersonCameraController camController;
	public float delta;
	public ModelLoader loader = new ObjLoader();
	public Model bulletmodel;
	public ModelInstance bulletdraw;
	public float width;
	public float height;
	public BitmapFont font;
	Model handmodel;
	ModelInstance hand;
	public class HapzCamera extends PerspectiveCamera {
		public Vector3 right;

		final Vector3 tmp = new Vector3();
		public HapzCamera (float fieldOfViewY, float viewportWidth, float viewportHeight) {
			this.fieldOfView = fieldOfViewY;
			this.viewportWidth = viewportWidth;
			this.viewportHeight = viewportHeight;
			this.right = new Vector3();
			update();
		}
		@Override
		public void update () {
			super.update();
			right.set((this.direction).cpy().crs(this.up).nor());
		}

		@Override
		public void update (boolean updateFrustum) {
			super.update(updateFrustum);
			/** Hapz Additions below **/
			right.set((this.direction).cpy().crs(this.up).nor());
		}
	}

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


// camera.up.rotate(tmp, deltaY);
			return true;
		}

		public void update (float deltaTime) {

			float deltaX = -Gdx.input.getDeltaX() * degreesPerPixel;
			float deltaY = -Gdx.input.getDeltaY() * degreesPerPixel;
			camera.direction.rotate(camera.up, deltaX).nor();
			if ((!(camera.direction.dot(camera.up) > 0.99d && deltaY > 0) &&
					!(camera.direction.dot(-camera.up.x, -camera.up.y, -camera.up.z) > 0.99d && deltaY < 0))) {
				tmp.set(camera.direction).crs(camera.up).nor();
				camera.direction.rotate(tmp, deltaY);
			}

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
		HapzCamera view;
		//controller defines movement, speed, and... controls
		FirstPersonCameraController controller;
		float fireTimer = 0f; // timer for ROF
		float ROF = 70F; //how long till next bullet
		float aimSpeed = 15f; //speed in the direction of firing
		float spread = 15f; //radius speed
		float hitSize = 0.5f; // size of hitbox
		float health = 5f;
		float atkHor = 4.5f;
		float atkFor = 6f;

		/* TODO:dmg, hp, etc
		 TODO: add models, textures, music, etc
		 TODO: S T O R Y L I N E
		 TODO: knockback resistance
		 TODO: animation.
		 TODO: ACTUAL CHARACTERS!!! */
	}

	public class Monster extends Character {

	}

	void spiral(Character owner,float x,float y,float z){
		// TODO: fix bullet spaghetti
		Bullet newbul = new Bullet();
		bullets.add(newbul);
		//set players
		newbul.owner = owner;
		//set position
		newbul.pos.set(owner.view.position).mulAdd(owner.view.direction,z).mulAdd(owner.view.right,x).mulAdd(owner.view.up,y);
		//apply wiggly vel dir
		newbul.vel.set(owner.view.direction).scl(owner.aimSpeed).mulAdd(owner.view.up,owner.spread*MathUtils.sin(timer)).mulAdd(owner.view.right,owner.spread*MathUtils.cos(timer));
		//sped = bullets.get(bullets.size-1).vel.set(player.view.direction).scl(sped).add(vert).add(hor).scl(delta).scl(player.ROF).len();
		//set time until next bullet
		owner.fireTimer = owner.ROF*delta;
	}

	public static class Bullet {
		Vector3 pos = new Vector3(0,0,0);
		Vector3 vel = new Vector3(0,0,0);
		Character owner;
	}
	final Array<Bullet> bullets = new Array<Bullet>();
	
	Character player = new Character();
	float timer = 0f;
	@Override public void create() {
		//font
		font = new BitmapFont();
		//basic vars & setup
		width = Gdx.graphics.getWidth();
		height = Gdx.graphics.getHeight();
		Gdx.input.setCursorCatched(true);
		Gdx.input.setCursorPosition(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
		final float sensM = 6.2918f/2;
		//universal bullet model & instant
		bulletmodel = loader.loadModel(Gdx.files.internal("3d/basic_bullet/model.obj"));
		bulletdraw = new ModelInstance(bulletmodel);
		//chars have cams and controllers to simplify player & controls
		//"view" is char cams
		handmodel = loader.loadModel(Gdx.files.internal("3d/player/mitten.obj"));
		hand = new ModelInstance(handmodel);
		player.view = new HapzCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		player.view.position.set(0,0,0);
		player.view.near = 0.25f;
		player.view.far = 150f;
		//player controller
		player.controller = new FirstPersonCameraController(player.view);
		player.controller.setDegreesPerPixel(1/sensM);
		player.controller.setVelocity(15);
		player.view.update(true);

		modelBatch = new ModelBatch();
		spriteBatch = new SpriteBatch();

		// TODO: add more arenas (aesthetic)

		arena_model = loader.loadModel(Gdx.files.internal("3d/arenas/basic/model.obj"));
		arena = new ModelInstance(arena_model);


		Gdx.input.setInputProcessor(player.controller);

	}

	public void update() {
		delta = Gdx.graphics.getDeltaTime();
		timer += delta;
		player.fireTimer -= delta;
		if( bullets.size < 10000 & Gdx.input.isButtonPressed(Input.Buttons.LEFT) & player.fireTimer < 1){
			spiral(player,-player.atkHor,0, player.atkFor);
		}
		if(player.fireTimer > player.ROF*delta){player.fireTimer = player.ROF*delta;}
		if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) {
			Gdx.app.exit();
		}
		player.view.update();
		player.controller.update(delta);
		for(int i = bullets.size-1; i >= 0; i--){
			if(bullets.get(i).pos.dst(player.view.position) >= player.view.far)
			{bullets.removeIndex(i);} else
				{bullets.get(i).pos.mulAdd(bullets.get(i).vel,delta);
					if((player.view.position.dst(bullets.get(i).pos) < player.hitSize+1) & (bullets.get(i).owner != player)){
						player.health -= 1f; // TODO: bullet effects
						bullets.removeIndex(i);
					}
				}
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
		} //repeat;
		Vector3 transform = player.view.position.cpy().mulAdd(player.view.right,-player.atkHor);
		hand.transform.setToTranslation(transform.mulAdd(player.view.direction,player.atkFor-0.5f));
		hand.transform.rotateTowardTarget(transform.mulAdd(player.view.direction,player.atkFor),player.view.up);
		modelBatch.render(hand);
		transform = player.view.position.cpy().mulAdd(player.view.right,player.atkHor);
		hand.transform.setToTranslation(transform.mulAdd(player.view.direction,player.atkFor-0.5f));
		hand.transform.rotateTowardTarget(transform.mulAdd(player.view.direction,player.atkFor),player.view.up);
		hand.transform.scale(-1,1,1);
		modelBatch.render(hand);
		modelBatch.end();
		spriteBatch.begin();
				font.draw(spriteBatch,"health:" + player.health,0,height-font.getScaleY());
		spriteBatch.end();

	}

	@Override
	//TODO: throw away your trash, peasant
	public void dispose() {
		modelBatch.dispose();
		arena_model.dispose();
		bulletmodel.dispose();
	}




}
