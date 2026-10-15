from blog.models import Post
from rest_framework import serializers

class PostSerializer(serializers.ModelSerializer):
    liked = serializers.SerializerMethodField()

    def get_liked(self, obj):
        request = self.context['request']
        if request.user.is_authenticated:
            return obj.liked_by.filter(pk=request.user.pk).exists()
        return False

    class Meta:
        model = Post
        fields = ('id', 'liked', 'author', 'title', 'text', 'created_date', 'published_date', 'image')
