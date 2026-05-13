"""create_embeddings_table

Revision ID: e77208f4a3d7
Revises: 
Create Date: 2026-05-13 11:32:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from pgvector.sqlalchemy import Vector

# revision identifiers, used by Alembic.
revision: str = 'e77208f4a3d7'
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Enable pgvector extension
    op.execute("CREATE EXTENSION IF NOT EXISTS vector")
    
    op.create_table(
        'embeddings',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('source_id', sa.String(), nullable=False),
        sa.Column('source_type', sa.String(), nullable=False),
        sa.Column('vector', Vector(1536), nullable=False),
        sa.Column('created_at', sa.DateTime(), nullable=False),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_embeddings_source_id'), 'embeddings', ['source_id'], unique=False)


def downgrade() -> None:
    op.drop_index(op.f('ix_embeddings_source_id'), table_name='embeddings')
    op.drop_table('embeddings')
    # op.execute("DROP EXTENSION vector") # Optional: usually safer to keep it
