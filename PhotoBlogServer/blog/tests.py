from django.contrib.auth import get_user_model
from django.test import TestCase
from django.urls import reverse
from django.utils import timezone
from rest_framework import status
from rest_framework.authtoken.models import Token
from rest_framework.test import APITestCase

from .models import Post


class PostModelTests(TestCase):
    def setUp(self):
        self.user = get_user_model().objects.create_user(username='admin', password='1234')
        self.post = Post.objects.create(author=self.user, title='첫 번째 게시글', text='게시글 본문')

    def test_publish(self):
        self.post.publish()
        published_post = Post.objects.get(pk=self.post.pk)

        self.assertIsNotNone(published_post.published_date)

    def test_str(self):
        self.assertEqual(str(self.post), self.post.title)


class PostAPITests(APITestCase):
    def setUp(self):
        self.user = get_user_model().objects.create_user(username='admin', password='1234')
        self.token = Token.objects.create(user=self.user)
        self.client.credentials(HTTP_AUTHORIZATION=f'Token {self.token.key}')

        self.post = Post.objects.create(author=self.user, title='첫 번째 게시글', text='게시글 본문')
        self.list_url = reverse('post-list')
        self.detail_url = reverse('post-detail', args=[self.post.pk])

    def test_list_posts(self):
        self.client.credentials()
        response = self.client.get(self.list_url)
        
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['count'], 1)
        self.assertEqual(len(response.data['results']), 1)

    def test_retrieve_post(self):
        self.client.credentials()
        response = self.client.get(self.detail_url)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['author'], self.user.pk)
        self.assertEqual(response.data['title'], self.post.title)
        self.assertEqual(response.data['text'], self.post.text)

    def test_create_post(self):
        payload = {
            'author': self.user.pk,
            'title': '새 게시글',
            'text': '새 게시글 본문',
        }
        response = self.client.post(self.list_url, payload, format='json')

        # 포스트들 중에서 방금 생성한 거 찾기
        created_post = None
        for post in Post.objects.all():
            if post.pk != self.post.pk:
                created_post = post
                break

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(Post.objects.count(), 2)
        self.assertEqual(created_post.author, self.user)
        self.assertEqual(created_post.title, payload['title'])
        self.assertEqual(created_post.text, payload['text'])

    def test_update_post(self):
        payload = {'title': '수정한 제목', 'text': '수정한 본문'}

        response = self.client.patch(self.detail_url, payload, format='json')

        # 새로 조회해서 수정된 정보 반영된 객체 다시 불러옴
        updated_post = Post.objects.get(pk=self.post.pk)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(updated_post.title, payload['title'])
        self.assertEqual(updated_post.text, payload['text'])
        self.assertEqual(response.data['title'], payload['title'])
        self.assertEqual(response.data['text'], payload['text'])
        self.assertEqual(updated_post.author, self.user)
        self.assertEqual(Post.objects.count(), 1)

    def test_delete_post(self):
        response = self.client.delete(self.detail_url)

        deleted_post = Post.objects.filter(pk=self.post.pk).first()

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertIsNone(deleted_post)
