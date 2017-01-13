/**
  * Created by aknay on 6/1/17.
  */

import org.scalatestplus.play.{OneAppPerSuite, OneAppPerTest, PlaySpec}
import play.api.Application
import dao.{AlbumDao, UserDao}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}


class ModelSpec extends PlaySpec with BeforeAndAfterEach with OneAppPerSuite {

  import models._

  def userDao(implicit app: Application) = {
    val app2UserDAO = Application.instanceCache[UserDao]
    app2UserDAO(app)
  }

  def albumDao(implicit app: Application) = {
    val app2AlbumDAO = Application.instanceCache[AlbumDao]
    app2AlbumDAO(app)
  }

  val EMAIL_NAME1 = "qwe@qwe.com"
  val PASSWORD1 = "qwe"
  val EMAIL_NAME2 = "abc@abc.com"
  val PASSWORD2 = "abc"

  override def beforeEach(): Unit = {
    userDao.createUserTableIfNotExisted
    userDao.createUserInfoTableIfNotExisted
  }

  "User Model" should {
    "User Dao" should {
      "insert data and check its existence" in {
        val user = User(Some(99), EMAIL_NAME1, "qwe")
        userDao.insertUserWithUserInfo(user) mustBe true
        userDao.insertUserWithUserInfo(user) mustBe false
      }
    }
  }

  "Album Model" should {
    "Album Dao" should {
      "insert data and check its existence" in {
        val user = User(Some(99), EMAIL_NAME1, "qwe")
        userDao.insertUserWithUserInfo(user) mustBe true
        val userTemp = userDao.getUserByEmailAddress(EMAIL_NAME1);
        userTemp.isDefined mustBe true
        val album = Album(Some(1), userTemp.get.id, "ARTIST", "TITLE")

        albumDao.isAlbumExisted(album, userTemp.get.id.get) mustBe false //should not exist before we insert an album
        if (userTemp.isDefined) albumDao.insertAlbum(album, userTemp.get.id.get)
        albumDao.isAlbumExisted(album, userTemp.get.id.get) mustBe true //should exist after we insert the album
        albumDao.delete(album, userTemp.get.id.get)
        albumDao.isAlbumExisted(album, userTemp.get.id.get) mustBe false //should not exist after the album is deleted

        albumDao.insertAlbum(album, userTemp.get.id.get)
        albumDao.isAlbumExisted(album, userTemp.get.id.get) mustBe true //should exist after we insert the album
        userDao.deleteUser(EMAIL_NAME1)
        albumDao.isAlbumExisted(album, userTemp.get.id.get) mustBe false //should not exist after we delete the user
      }
    }


    "Album Dao" should {
      "insert data and check its existence for two user with same album and artist" in {
        val user1 = User(Some(99), EMAIL_NAME1, PASSWORD1)
        userDao.insertUserWithUserInfo(user1) mustBe true

        val user2 = User(Some(100), EMAIL_NAME2, PASSWORD2)
        userDao.insertUserWithUserInfo(user2) mustBe true

        val userTemp1 = userDao.getUserByEmailAddress(EMAIL_NAME1);
        userTemp1.isDefined mustBe true

        val userTemp2 = userDao.getUserByEmailAddress(EMAIL_NAME2);
        userTemp2.isDefined mustBe true


        val album1 = Album(Some(1), userTemp1.get.id, "ARTIST", "TITLE")
        val album2 = Album(Some(2), userTemp2.get.id, "ARTIST", "TITLE")

        albumDao.isAlbumExisted(album1, userTemp1.get.id.get) mustBe false //should not exist before we insert an album
        albumDao.isAlbumExisted(album2, userTemp2.get.id.get) mustBe false //should not exist before we insert an album

        albumDao.insertAlbum(album1, userTemp1.get.id.get)
        albumDao.insertAlbum(album2, userTemp2.get.id.get)


        albumDao.isAlbumExisted(album1, userTemp1.get.id.get) mustBe true //should exist after we insert the album
        albumDao.delete(album1, userTemp1.get.id.get)
        albumDao.isAlbumExisted(album1, userTemp1.get.id.get) mustBe false //should not exist after the album is deleted
        albumDao.isAlbumExisted(album2, userTemp2.get.id.get) mustBe true //user2 should still has album2 even album1 with same artist and title are deleted
        albumDao.isAlbumExisted(album2, userTemp1.get.id.get) mustBe false //user1 should not have album2 info also even it has same artist and title
        albumDao.isAlbumExisted(album1, userTemp2.get.id.get) mustBe true //should be true since we have same artist and title
      }
    }

    "Album Dao" should {
      "insert data and check its existence for one user with two album" in {
        val user1 = User(Some(99), EMAIL_NAME1, PASSWORD1)
        userDao.insertUserWithUserInfo(user1) mustBe true

        val userTemp1 = userDao.getUserByEmailAddress(EMAIL_NAME1);
        userTemp1.isDefined mustBe true

        val album1 = Album(Some(1), userTemp1.get.id, "ARTIST", "TITLE")
        val album2 = Album(Some(2), userTemp1.get.id, "ARTIST", "TITLE")
        val album3 = Album(Some(3), userTemp1.get.id, "ARTIST_ONE", "TITLE")
        val album4 = Album(Some(4), userTemp1.get.id, "ARTIST", "TITLE_ONE")

        albumDao.insertAlbum(album1, userTemp1.get.id.get) mustBe true
        albumDao.insertAlbum(album2, userTemp1.get.id.get) mustBe false //we can't add  both same artist and title name
        albumDao.insertAlbum(album3, userTemp1.get.id.get) mustBe true //we can add different artist with same title name
        albumDao.insertAlbum(album4, userTemp1.get.id.get) mustBe true //we can add same artist with different title name
      }
    }


    "Album Dao" should {
      "should retrieve albums" in {
        val user1 = User(Some(99), EMAIL_NAME1, PASSWORD1)
        userDao.insertUserWithUserInfo(user1) mustBe true

        val userTemp1 = userDao.getUserByEmailAddress(EMAIL_NAME1);
        userTemp1.isDefined mustBe true

        val album1 = Album(Some(1), userTemp1.get.id, "ARTIST", "TITLE")
        val album2 = Album(Some(2), userTemp1.get.id, "ARTIST_ONE", "TITLE")
        val album3 = Album(Some(3), userTemp1.get.id, "ARTIST", "TITLE_ONE")

        albumDao.insertAlbum(album1, userTemp1.get.id.get)
        albumDao.insertAlbum(album2, userTemp1.get.id.get)
        albumDao.insertAlbum(album3, userTemp1.get.id.get)

        albumDao.retrieveByUserId(userTemp1.get.id.get).foreach(println)
        val albumSeq = albumDao.retrieveByUserId(userTemp1.get.id.get)

        albumSeq.contains(("ARTIST", "TITLE")) mustBe true
        albumSeq.contains(("ARTIST_ONE", "TITLE")) mustBe true
        albumSeq.contains(("ARTIST", "TITLE_ONE")) mustBe true
        albumSeq.contains(("ARTIST_UNKNOWN", "TITLE")) mustBe false

      }
    }


    "Album Dao" should {
      "should update albums" in {
        val user1 = User(Some(99), EMAIL_NAME1, PASSWORD1)
        userDao.insertUserWithUserInfo(user1) mustBe true

        val userTemp1 = userDao.getUserByEmailAddress(EMAIL_NAME1);
        userTemp1.isDefined mustBe true

        val album1 = Album(Some(1), userTemp1.get.id, "ARTIST", "TITLE")
        val album2 = Album(Some(2), userTemp1.get.id, "ARTIST_ONE", "TITLE")
        val album3 = Album(Some(3), userTemp1.get.id, "ARTIST", "TITLE_ONE")

        albumDao.insertAlbum(album1, userTemp1.get.id.get)
        albumDao.insertAlbum(album2, userTemp1.get.id.get)
        albumDao.insertAlbum(album3, userTemp1.get.id.get)
        val ARTIST_NAME_TO_UPDATE = "UPDATED ARTIST NAME"
        val TITLE_TO_UPDATE = "UPDATED TITLE"
        val album1Update = Album(Some(4), userTemp1.get.id, ARTIST_NAME_TO_UPDATE, TITLE_TO_UPDATE)

        albumDao.update(album1.id.get, album1Update, userTemp1.get.id.get)

        albumDao.retrieveByUserId(userTemp1.get.id.get).foreach(println)
        val albumSeq = albumDao.retrieveByUserId(userTemp1.get.id.get)

        albumSeq.contains(("ARTIST", "TITLE")) mustBe true
        albumSeq.contains(("ARTIST_ONE", "TITLE")) mustBe true
        albumSeq.contains(("ARTIST", "TITLE_ONE")) mustBe true
        albumSeq.contains(("ARTIST_UNKNOWN", "TITLE")) mustBe false

        val albumId = albumDao.retrieveAlbumId("ARTIST", "TITLE", userTemp1.get.id.get)
        albumId.isDefined mustBe true
        albumDao.update(albumId.get, album1Update, userTemp1.get.id.get)

        albumDao.retrieveByUserId(userTemp1.get.id.get).contains((ARTIST_NAME_TO_UPDATE, TITLE_TO_UPDATE)) mustBe true
        albumDao.retrieveByUserId(userTemp1.get.id.get).size mustBe 3 //we only inserted three albums
      }
    }

  }

  override def afterEach(): Unit = {
    userDao.deleteUser(EMAIL_NAME1)
    userDao.deleteUser(EMAIL_NAME2)
  }
}